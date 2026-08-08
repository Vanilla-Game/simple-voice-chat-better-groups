package ru.vanillagame.voicechat.grouptools;

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
import java.util.List;
import java.util.Locale;
import java.util.UUID;

final class VcGroupCommand implements CommandExecutor, TabCompleter {

    private static final String PERMISSION = "vanillagame.voicechat_group_tools.use";

    private final VoiceChatGroupToolsPlugin plugin;
    private final InviteStore invites;
    private final GroupOwnershipRegistry ownership;

    VcGroupCommand(
            VoiceChatGroupToolsPlugin plugin,
            InviteStore invites,
            GroupOwnershipRegistry ownership
    ) {
        this.plugin = plugin;
        this.invites = invites;
        this.ownership = ownership;
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
        if (args.length != 2) {
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
                        .append(Messages.component(Messages.INVITE_EXPIRES, NamedTextColor.GRAY))
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

    private void kick(Player creator, String targetName, VoicechatServerApi api) {
        VoicechatConnection creatorConnection = api.getConnectionOf(creator.getUniqueId());
        if (creatorConnection == null) {
            creator.sendMessage(Messages.component(Messages.CONNECTION_SELF_UNAVAILABLE, NamedTextColor.RED));
            return;
        }

        Group creatorGroup = creatorConnection.getGroup();
        if (creatorGroup == null) {
            creator.sendMessage(Messages.component(Messages.KICK_SENDER_NOT_IN_GROUP, NamedTextColor.RED));
            return;
        }

        GroupOwnershipRegistry.Authorization authorization =
                ownership.authorize(creatorGroup.getId(), creator.getUniqueId());
        if (authorization == GroupOwnershipRegistry.Authorization.UNKNOWN_CREATOR) {
            creator.sendMessage(Messages.component(Messages.KICK_UNKNOWN_CREATOR, NamedTextColor.RED));
            return;
        }
        if (authorization == GroupOwnershipRegistry.Authorization.NOT_CREATOR) {
            creator.sendMessage(Messages.component(Messages.KICK_NOT_CREATOR, NamedTextColor.RED));
            return;
        }

        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) {
            creator.sendMessage(Messages.component(Messages.PLAYER_NOT_ONLINE, NamedTextColor.RED));
            return;
        }
        if (target.getUniqueId().equals(creator.getUniqueId())) {
            creator.sendMessage(Messages.component(Messages.KICK_SELF, NamedTextColor.RED));
            return;
        }

        VoicechatConnection targetConnection = api.getConnectionOf(target.getUniqueId());
        Group targetGroup = targetConnection == null ? null : targetConnection.getGroup();
        if (targetGroup == null || !targetGroup.getId().equals(creatorGroup.getId())) {
            creator.sendMessage(Messages.component(
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
            creator.sendMessage(Messages.component(Messages.KICK_FAILED, NamedTextColor.RED));
            return;
        }

        creator.sendMessage(Messages.component(
                Messages.KICK_SUCCESS,
                NamedTextColor.GREEN,
                Component.text(target.getName())
        ));
        target.sendMessage(Messages.component(Messages.KICK_TARGET_NOTIFICATION, NamedTextColor.YELLOW));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filter(List.of("invite", "accept", "kick"), args[0]);
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("invite") || args[0].equalsIgnoreCase("kick"))) {
            List<String> names = new ArrayList<>();
            for (Player player : Bukkit.getOnlinePlayers()) {
                names.add(player.getName());
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
