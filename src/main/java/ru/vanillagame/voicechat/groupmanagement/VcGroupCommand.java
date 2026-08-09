package ru.vanillagame.voicechat.groupmanagement;

import de.maxhenkel.voicechat.api.Group;
import de.maxhenkel.voicechat.api.VoicechatConnection;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

final class VcGroupCommand implements CommandExecutor, TabCompleter {

    private static final String PERMISSION = "vanillagame.svc_group_management.use";

    private final SvcGroupManagementPlugin plugin;
    private final InviteStore invites;
    private final GroupLeadershipRegistry leadership;
    private final InviteCooldownStore inviteCooldowns;
    private final RequestStore requests;
    private final InviteCooldownStore requestCooldowns;
    private final PluginSettings settings;

    VcGroupCommand(
            SvcGroupManagementPlugin plugin,
            InviteStore invites,
            GroupLeadershipRegistry leadership,
            InviteCooldownStore inviteCooldowns,
            RequestStore requests,
            InviteCooldownStore requestCooldowns,
            PluginSettings settings
    ) {
        this.plugin = plugin;
        this.invites = invites;
        this.leadership = leadership;
        this.inviteCooldowns = inviteCooldowns;
        this.requests = requests;
        this.requestCooldowns = requestCooldowns;
        this.settings = settings;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission(PERMISSION)) {
            sender.sendMessage(Messages.component(Messages.COMMAND_NO_PERMISSION, NamedTextColor.RED));
            return true;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Messages.component(Messages.COMMAND_PLAYERS_ONLY, NamedTextColor.RED));
            return true;
        }
        // "request" accepts multi-word group names; every other subcommand takes
        // exactly one argument.
        boolean isRequest = args.length >= 1 && args[0].equalsIgnoreCase("request");
        if (args.length < 2 || (!isRequest && args.length != 2)) {
            player.sendMessage(Messages.component(Messages.COMMAND_USAGE, NamedTextColor.YELLOW));
            return true;
        }

        VoicechatServerApi api = plugin.getVoicechatApi();
        if (api == null) {
            player.sendMessage(Messages.component(Messages.VOICECHAT_NOT_READY, NamedTextColor.RED));
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "invite" -> invite(player, args[1], api);
            case "accept" -> accept(player, args[1], api);
            case "kick" -> kick(player, args[1], api);
            case "transfer" -> transfer(player, args[1], api);
            case "request" -> request(player, String.join(" ", Arrays.copyOfRange(args, 1, args.length)), api);
            case "approve" -> approve(player, args[1], api);
            default -> player.sendMessage(Messages.component(Messages.COMMAND_USAGE, NamedTextColor.YELLOW));
        }
        return true;
    }

    private void invite(Player inviter, String targetName, VoicechatServerApi api) {
        VoicechatConnection inviterConnection = api.getConnectionOf(inviter.getUniqueId());
        if (inviterConnection == null) {
            inviter.sendMessage(Messages.component(Messages.CONNECTION_SELF_UNAVAILABLE, NamedTextColor.RED));
            return;
        }

        Group inviterGroup = inviterConnection.getGroup();
        if (inviterGroup == null) {
            inviter.sendMessage(Messages.component(Messages.INVITE_SENDER_NOT_IN_GROUP, NamedTextColor.RED));
            return;
        }

        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) {
            inviter.sendMessage(Messages.component(Messages.PLAYER_NOT_ONLINE, NamedTextColor.RED));
            return;
        }
        if (target.getUniqueId().equals(inviter.getUniqueId())) {
            inviter.sendMessage(Messages.component(Messages.INVITE_SELF, NamedTextColor.RED));
            return;
        }

        VoicechatConnection targetConnection = api.getConnectionOf(target.getUniqueId());
        if (targetConnection == null) {
            inviter.sendMessage(Messages.component(Messages.CONNECTION_TARGET_UNAVAILABLE, NamedTextColor.RED));
            return;
        }
        if (targetConnection.getGroup() != null) {
            inviter.sendMessage(Messages.component(
                    Messages.GROUP_TARGET_ALREADY_IN_GROUP,
                    NamedTextColor.RED,
                    Component.text(target.getName())
            ));
            return;
        }

        UUID groupId = inviterGroup.getId();
        if (api.getGroup(groupId) == null) {
            inviter.sendMessage(Messages.component(Messages.GROUP_NO_LONGER_EXISTS, NamedTextColor.RED));
            return;
        }

        InviteCooldownStore.Attempt cooldownAttempt =
                inviteCooldowns.tryAcquire(inviter.getUniqueId(), target.getUniqueId());
        if (!cooldownAttempt.allowed()) {
            inviter.sendMessage(Messages.component(
                    Messages.INVITE_COOLDOWN,
                    NamedTextColor.RED,
                    Component.text(cooldownAttempt.retryAfterSeconds())
            ));
            return;
        }

        String token = invites.create(target.getUniqueId(), groupId);
        Component acceptButton = Messages.component(Messages.INVITE_ACCEPT_LABEL, NamedTextColor.GREEN)
                .decorate(TextDecoration.BOLD)
                .clickEvent(ClickEvent.runCommand("/vcgroup accept " + token))
                .hoverEvent(Messages.component(Messages.INVITE_ACCEPT_HOVER, NamedTextColor.GRAY));
        target.sendMessage(
                Messages.component(
                                Messages.INVITE_RECEIVED,
                                NamedTextColor.YELLOW,
                                Component.text(inviter.getName())
                        )
                        .append(Component.space())
                        .append(acceptButton)
                        .append(Component.space())
                        .append(Messages.component(
                                Messages.INVITE_EXPIRES,
                                NamedTextColor.GRAY,
                                Component.text(settings.inviteExpirationMinutes())
                        ))
        );
        inviter.sendMessage(Messages.component(
                Messages.INVITE_SENT,
                NamedTextColor.GREEN,
                Component.text(target.getName())
        ));
    }

    private void accept(Player player, String token, VoicechatServerApi api) {
        InviteStore.Lookup lookup = invites.lookup(token, player.getUniqueId());
        switch (lookup.status()) {
            case NOT_FOUND -> {
                player.sendMessage(Messages.component(Messages.INVITE_NOT_FOUND, NamedTextColor.RED));
                return;
            }
            case EXPIRED -> {
                player.sendMessage(Messages.component(Messages.INVITE_EXPIRED, NamedTextColor.RED));
                return;
            }
            case WRONG_PLAYER -> {
                player.sendMessage(Messages.component(Messages.INVITE_WRONG_PLAYER, NamedTextColor.RED));
                return;
            }
            case VALID -> {
                // Continue with live API validation below.
            }
        }

        InviteStore.Invite invite = lookup.invite();
        Group group = api.getGroup(invite.groupId());
        if (group == null) {
            invites.invalidateGroup(invite.groupId());
            player.sendMessage(Messages.component(Messages.GROUP_NO_LONGER_EXISTS, NamedTextColor.RED));
            return;
        }

        VoicechatConnection connection = api.getConnectionOf(player.getUniqueId());
        if (connection == null) {
            player.sendMessage(Messages.component(Messages.CONNECTION_SELF_UNAVAILABLE, NamedTextColor.RED));
            return;
        }
        if (connection.getGroup() != null) {
            invites.invalidatePlayer(player.getUniqueId());
            player.sendMessage(Messages.component(Messages.GROUP_ALREADY_IN_GROUP, NamedTextColor.RED));
            return;
        }

        connection.setGroup(group);
        // VoicechatConnection is a snapshot. Re-fetch it after a mutation to observe the new state.
        VoicechatConnection updatedConnection = api.getConnectionOf(player.getUniqueId());
        Group joinedGroup = updatedConnection == null ? null : updatedConnection.getGroup();
        if (joinedGroup == null || !joinedGroup.getId().equals(invite.groupId())) {
            player.sendMessage(Messages.component(Messages.GROUP_JOIN_FAILED, NamedTextColor.RED));
            return;
        }

        invites.consume(token, invite);
        invites.invalidatePlayer(player.getUniqueId());
        player.sendMessage(Messages.component(Messages.GROUP_JOINED, NamedTextColor.GREEN));
    }

    private void kick(Player leader, String targetName, VoicechatServerApi api) {
        VoicechatConnection leaderConnection = api.getConnectionOf(leader.getUniqueId());
        if (leaderConnection == null) {
            leader.sendMessage(Messages.component(Messages.CONNECTION_SELF_UNAVAILABLE, NamedTextColor.RED));
            return;
        }

        Group leaderGroup = leaderConnection.getGroup();
        if (leaderGroup == null) {
            leader.sendMessage(Messages.component(Messages.KICK_SENDER_NOT_IN_GROUP, NamedTextColor.RED));
            return;
        }

        GroupLeadershipRegistry.Authorization authorization =
                leadership.authorize(leaderGroup.getId(), leader.getUniqueId());
        if (authorization == GroupLeadershipRegistry.Authorization.UNKNOWN_LEADER) {
            leader.sendMessage(Messages.component(Messages.KICK_UNKNOWN_LEADER, NamedTextColor.RED));
            return;
        }
        if (authorization == GroupLeadershipRegistry.Authorization.NOT_LEADER) {
            leader.sendMessage(Messages.component(Messages.KICK_NOT_LEADER, NamedTextColor.RED));
            return;
        }

        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) {
            leader.sendMessage(Messages.component(Messages.PLAYER_NOT_ONLINE, NamedTextColor.RED));
            return;
        }
        if (target.getUniqueId().equals(leader.getUniqueId())) {
            leader.sendMessage(Messages.component(Messages.KICK_SELF, NamedTextColor.RED));
            return;
        }

        VoicechatConnection targetConnection = api.getConnectionOf(target.getUniqueId());
        Group targetGroup = targetConnection == null ? null : targetConnection.getGroup();
        if (targetGroup == null || !targetGroup.getId().equals(leaderGroup.getId())) {
            leader.sendMessage(Messages.component(
                    Messages.KICK_TARGET_NOT_MEMBER,
                    NamedTextColor.RED,
                    Component.text(target.getName())
            ));
            return;
        }

        targetConnection.setGroup(null);
        // VoicechatConnection is a snapshot. Re-fetch it after a mutation to observe the new state.
        VoicechatConnection updatedTargetConnection = api.getConnectionOf(target.getUniqueId());
        if (updatedTargetConnection == null || updatedTargetConnection.getGroup() != null) {
            leader.sendMessage(Messages.component(Messages.KICK_FAILED, NamedTextColor.RED));
            return;
        }

        leader.sendMessage(Messages.component(
                Messages.KICK_SUCCESS,
                NamedTextColor.GREEN,
                Component.text(target.getName())
        ));
        target.sendMessage(Messages.component(Messages.KICK_TARGET_NOTIFICATION, NamedTextColor.YELLOW));
    }

    private void transfer(Player leader, String targetName, VoicechatServerApi api) {
        VoicechatConnection leaderConnection = api.getConnectionOf(leader.getUniqueId());
        if (leaderConnection == null) {
            leader.sendMessage(Messages.component(Messages.CONNECTION_SELF_UNAVAILABLE, NamedTextColor.RED));
            return;
        }

        Group leaderGroup = leaderConnection.getGroup();
        if (leaderGroup == null) {
            leader.sendMessage(Messages.component(Messages.TRANSFER_SENDER_NOT_IN_GROUP, NamedTextColor.RED));
            return;
        }

        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) {
            leader.sendMessage(Messages.component(Messages.PLAYER_NOT_ONLINE, NamedTextColor.RED));
            return;
        }
        if (target.getUniqueId().equals(leader.getUniqueId())) {
            leader.sendMessage(Messages.component(Messages.TRANSFER_SELF, NamedTextColor.RED));
            return;
        }

        VoicechatConnection targetConnection = api.getConnectionOf(target.getUniqueId());
        Group targetGroup = targetConnection == null ? null : targetConnection.getGroup();
        if (targetGroup == null || !targetGroup.getId().equals(leaderGroup.getId())) {
            leader.sendMessage(Messages.component(
                    Messages.TRANSFER_TARGET_NOT_MEMBER,
                    NamedTextColor.RED,
                    Component.text(target.getName())
            ));
            return;
        }

        GroupLeadershipRegistry.TransferResult result = leadership.transferLeadership(
                leaderGroup.getId(),
                leader.getUniqueId(),
                target.getUniqueId()
        );
        switch (result.status()) {
            case UNKNOWN_LEADER -> leader.sendMessage(
                    Messages.component(Messages.TRANSFER_UNKNOWN_LEADER, NamedTextColor.RED));
            case NOT_LEADER -> leader.sendMessage(
                    Messages.component(Messages.TRANSFER_NOT_LEADER, NamedTextColor.RED));
            case TARGET_NOT_MEMBER -> leader.sendMessage(Messages.component(
                    Messages.TRANSFER_TARGET_NOT_MEMBER,
                    NamedTextColor.RED,
                    Component.text(target.getName())
            ));
            case TRANSFERRED -> {
                // The promotion notice to the new leader is sent by the leadership
                // sync pipeline together with the state broadcast.
                plugin.publishLeadership(result.transition());
                leader.sendMessage(Messages.component(
                        Messages.TRANSFER_SUCCESS,
                        NamedTextColor.GREEN,
                        Component.text(target.getName())
                ));
            }
        }
    }

    private void request(Player requester, String groupArg, VoicechatServerApi api) {
        VoicechatConnection connection = api.getConnectionOf(requester.getUniqueId());
        if (connection == null) {
            requester.sendMessage(Messages.component(Messages.CONNECTION_SELF_UNAVAILABLE, NamedTextColor.RED));
            return;
        }
        if (connection.getGroup() != null) {
            requester.sendMessage(Messages.component(Messages.GROUP_ALREADY_IN_GROUP, NamedTextColor.RED));
            return;
        }

        GroupResolution resolution = resolveVisibleGroup(groupArg, api);
        if (resolution.ambiguous()) {
            requester.sendMessage(Messages.component(Messages.REQUEST_GROUP_AMBIGUOUS, NamedTextColor.RED));
            return;
        }
        Group group = resolution.group();
        if (group == null) {
            requester.sendMessage(Messages.component(Messages.REQUEST_GROUP_NOT_FOUND, NamedTextColor.RED));
            return;
        }
        if (!group.hasPassword()) {
            requester.sendMessage(Messages.component(Messages.REQUEST_NOT_NEEDED, NamedTextColor.YELLOW));
            return;
        }

        UUID leaderId = leadership.leaderOf(group.getId());
        if (leaderId == null) {
            requester.sendMessage(Messages.component(Messages.REQUEST_UNKNOWN_LEADER, NamedTextColor.RED));
            return;
        }
        Player leader = Bukkit.getPlayer(leaderId);
        if (leader == null || !leader.isOnline()) {
            requester.sendMessage(Messages.component(Messages.REQUEST_LEADER_OFFLINE, NamedTextColor.RED));
            return;
        }

        InviteCooldownStore.Attempt cooldownAttempt =
                requestCooldowns.tryAcquire(requester.getUniqueId(), group.getId());
        if (!cooldownAttempt.allowed()) {
            requester.sendMessage(Messages.component(
                    Messages.REQUEST_COOLDOWN,
                    NamedTextColor.RED,
                    Component.text(cooldownAttempt.retryAfterSeconds())
            ));
            return;
        }

        String token = requests.create(requester.getUniqueId(), group.getId());
        Component acceptButton = Messages.component(Messages.REQUEST_ACCEPT_LABEL, NamedTextColor.GREEN)
                .decorate(TextDecoration.BOLD)
                .clickEvent(ClickEvent.runCommand("/vcgroup approve " + token))
                .hoverEvent(Messages.component(Messages.REQUEST_ACCEPT_HOVER, NamedTextColor.GRAY));
        leader.sendMessage(
                Messages.component(
                                Messages.REQUEST_RECEIVED,
                                NamedTextColor.YELLOW,
                                Component.text(requester.getName())
                        )
                        .append(Component.space())
                        .append(acceptButton)
                        .append(Component.space())
                        .append(Messages.component(
                                Messages.REQUEST_EXPIRES,
                                NamedTextColor.GRAY,
                                Component.text(settings.requestExpirationMinutes())
                        ))
        );
        requester.sendMessage(Messages.component(Messages.REQUEST_SENT, NamedTextColor.GREEN));
    }

    private void approve(Player approver, String token, VoicechatServerApi api) {
        RequestStore.Lookup lookup = requests.lookup(token);
        switch (lookup.status()) {
            case NOT_FOUND -> {
                approver.sendMessage(Messages.component(Messages.REQUEST_NOT_FOUND, NamedTextColor.RED));
                return;
            }
            case EXPIRED -> {
                approver.sendMessage(Messages.component(Messages.REQUEST_EXPIRED, NamedTextColor.RED));
                return;
            }
            case VALID -> {
                // Continue with live validation below.
            }
        }

        RequestStore.Request request = lookup.request();
        Group group = api.getGroup(request.groupId());
        if (group == null) {
            requests.invalidateGroup(request.groupId());
            approver.sendMessage(Messages.component(Messages.GROUP_NO_LONGER_EXISTS, NamedTextColor.RED));
            return;
        }

        // Approval is bound to current leadership at click time, not to whoever
        // received the request message.
        GroupLeadershipRegistry.Authorization authorization =
                leadership.authorize(request.groupId(), approver.getUniqueId());
        if (authorization != GroupLeadershipRegistry.Authorization.LEADER) {
            approver.sendMessage(Messages.component(Messages.APPROVE_NOT_LEADER, NamedTextColor.RED));
            return;
        }

        Player requester = Bukkit.getPlayer(request.requesterId());
        if (requester == null || !requester.isOnline()) {
            requests.consume(token, request);
            approver.sendMessage(Messages.component(Messages.APPROVE_REQUESTER_OFFLINE, NamedTextColor.RED));
            return;
        }
        VoicechatConnection requesterConnection = api.getConnectionOf(requester.getUniqueId());
        if (requesterConnection == null) {
            approver.sendMessage(Messages.component(Messages.CONNECTION_TARGET_UNAVAILABLE, NamedTextColor.RED));
            return;
        }
        if (requesterConnection.getGroup() != null) {
            requests.consume(token, request);
            approver.sendMessage(Messages.component(
                    Messages.APPROVE_REQUESTER_IN_GROUP,
                    NamedTextColor.RED,
                    Component.text(requester.getName())
            ));
            return;
        }

        requesterConnection.setGroup(group);
        // VoicechatConnection is a snapshot. Re-fetch it after a mutation to observe the new state.
        VoicechatConnection updatedConnection = api.getConnectionOf(requester.getUniqueId());
        Group joinedGroup = updatedConnection == null ? null : updatedConnection.getGroup();
        if (joinedGroup == null || !joinedGroup.getId().equals(request.groupId())) {
            approver.sendMessage(Messages.component(Messages.APPROVE_FAILED, NamedTextColor.RED));
            return;
        }

        requests.consume(token, request);
        requests.invalidateRequester(requester.getUniqueId());
        approver.sendMessage(Messages.component(
                Messages.APPROVE_SUCCESS,
                NamedTextColor.GREEN,
                Component.text(requester.getName())
        ));
        requester.sendMessage(Messages.component(Messages.APPROVE_JOINED, NamedTextColor.GREEN));
    }

    private record GroupResolution(Group group, boolean ambiguous) {

        static final GroupResolution NOT_FOUND = new GroupResolution(null, false);
        static final GroupResolution AMBIGUOUS = new GroupResolution(null, true);
    }

    private GroupResolution resolveVisibleGroup(String groupArg, VoicechatServerApi api) {
        try {
            Group byId = api.getGroup(UUID.fromString(groupArg));
            return byId == null || byId.isHidden()
                    ? GroupResolution.NOT_FOUND
                    : new GroupResolution(byId, false);
        } catch (IllegalArgumentException notAUuid) {
            // Fall through to name resolution.
        }

        Group match = null;
        for (Group candidate : api.getGroups()) {
            if (candidate.isHidden() || !candidate.getName().equals(groupArg)) {
                continue;
            }
            if (match != null) {
                return GroupResolution.AMBIGUOUS;
            }
            match = candidate;
        }
        return match == null ? GroupResolution.NOT_FOUND : new GroupResolution(match, false);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filter(List.of("invite", "accept", "kick", "transfer", "request"), args[0]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("request")) {
            VoicechatServerApi api = plugin.getVoicechatApi();
            if (api == null) {
                return List.of();
            }
            List<String> names = new ArrayList<>();
            for (Group group : api.getGroups()) {
                if (!group.isHidden() && group.hasPassword()) {
                    names.add(group.getName());
                }
            }
            return filter(names, args[1]);
        }
        if (args.length == 2
                && (args[0].equalsIgnoreCase("invite")
                        || args[0].equalsIgnoreCase("kick")
                        || args[0].equalsIgnoreCase("transfer"))) {
            List<String> names = new ArrayList<>();
            Player viewer = sender instanceof Player player ? player : null;
            for (Player candidate : Bukkit.getOnlinePlayers()) {
                if (viewer == null || viewer.canSee(candidate)) {
                    names.add(candidate.getName());
                }
            }
            return filter(names, args[1]);
        }
        return List.of();
    }

    private static List<String> filter(List<String> values, String prefix) {
        String normalizedPrefix = prefix.toLowerCase(Locale.ROOT);
        return values.stream()
                .filter(value -> value.toLowerCase(Locale.ROOT).startsWith(normalizedPrefix))
                .toList();
    }
}
