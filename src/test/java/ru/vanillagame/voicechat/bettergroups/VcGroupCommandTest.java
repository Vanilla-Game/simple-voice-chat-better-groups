package ru.vanillagame.voicechat.bettergroups;

import de.maxhenkel.voicechat.api.Group;
import de.maxhenkel.voicechat.api.VoicechatConnection;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class VcGroupCommandTest {

    private static final Instant NOW = Instant.parse("2026-08-08T10:00:00Z");

    @Test
    void inviteCooldownThrottlesRepeatInvitesToSameTargetOnly() {
        BetterGroupsPlugin plugin = mock(BetterGroupsPlugin.class);
        InviteStore invites = mock(InviteStore.class);
        GroupLeadershipRegistry leadership = new GroupLeadershipRegistry();
        InviteCooldownStore cooldowns = new InviteCooldownStore(
                Clock.fixed(NOW, ZoneOffset.UTC),
                Duration.ofSeconds(10)
        );
        VoicechatServerApi api = mock(VoicechatServerApi.class);
        Player inviter = player("Inviter");
        Player target = player("Target");
        Player other = player("Other");
        Group group = group();
        VoicechatConnection inviterConnection = connection(group);
        VoicechatConnection targetConnection = connection(null);
        VoicechatConnection otherConnection = connection(null);
        when(plugin.getVoicechatApi()).thenReturn(api);
        when(api.getConnectionOf(inviter.getUniqueId())).thenReturn(inviterConnection);
        when(api.getConnectionOf(target.getUniqueId())).thenReturn(targetConnection);
        when(api.getConnectionOf(other.getUniqueId())).thenReturn(otherConnection);
        when(api.getGroup(group.getId())).thenReturn(group);
        UUID targetId = target.getUniqueId();
        UUID otherId = other.getUniqueId();
        UUID groupIdValue = group.getId();
        when(invites.create(eq(targetId), eq(groupIdValue), any(), any())).thenReturn("token");
        when(invites.create(eq(otherId), eq(groupIdValue), any(), any())).thenReturn("token2");
        VcGroupCommand command = new VcGroupCommand(
                plugin,
                invites,
                leadership,
                cooldowns,
                new RequestStore(Clock.fixed(NOW, ZoneOffset.UTC), Duration.ofMinutes(5)),
                new InviteCooldownStore(Clock.fixed(NOW, ZoneOffset.UTC), Duration.ZERO),
                new PluginSettings(5, 10, "block.anvil.land", 1.0F, 1.0F, 5, 30, "block.anvil.land", 1.0F, 1.0F)
        );

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getPlayerExact("Target")).thenReturn(target);
            bukkit.when(() -> Bukkit.getPlayerExact("Other")).thenReturn(other);

            command.onCommand(inviter, mock(Command.class), "vcgroup", new String[]{"invite", "Target"});
            command.onCommand(inviter, mock(Command.class), "vcgroup", new String[]{"invite", "Target"});
            command.onCommand(inviter, mock(Command.class), "vcgroup", new String[]{"invite", "Other"});
        }

        verify(invites, times(1)).create(eq(targetId), eq(groupIdValue), any(), any());
        verify(target, times(1)).sendMessage(any(Component.class));
        verify(target, times(1)).playSound(
                any(net.kyori.adventure.sound.Sound.class),
                any(net.kyori.adventure.sound.Sound.Emitter.class)
        );
        verify(invites, times(1)).create(eq(otherId), eq(groupIdValue), any(), any());
        verify(other, times(1)).sendMessage(any(Component.class));
    }

    @Test
    void acceptMutatesThenRechecksSnapshotAndConsumesInvite() {
        BetterGroupsPlugin plugin = mock(BetterGroupsPlugin.class);
        UUID playerId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();
        Player player = player("Target", playerId);
        Group group = group(groupId);
        VoicechatConnection beforeJoin = connection(null);
        VoicechatConnection afterJoin = connection(group);
        VoicechatServerApi api = mock(VoicechatServerApi.class);
        when(plugin.getVoicechatApi()).thenReturn(api);
        when(api.getGroup(groupId)).thenReturn(group);
        when(api.getConnectionOf(playerId)).thenReturn(beforeJoin, afterJoin);
        InviteStore invites = new InviteStore(
                Clock.fixed(NOW, ZoneOffset.UTC),
                Duration.ofMinutes(5),
                () -> "one-time-token"
        );
        invites.create(playerId, groupId, UUID.randomUUID(), "Inviter");
        VcGroupCommand command = command(plugin, invites, new GroupLeadershipRegistry());

        command.onCommand(
                player,
                mock(Command.class),
                "vcgroup",
                new String[]{"accept", "one-time-token"}
        );

        verify(beforeJoin).setGroup(group);
        assertEquals(InviteStore.LookupStatus.NOT_FOUND, invites.lookup("one-time-token", playerId).status());
    }

    @Test
    void inviteReachesTargetsInOtherGroupsButNotOwnGroupMembers() {
        BetterGroupsPlugin plugin = mock(BetterGroupsPlugin.class);
        InviteStore invites = mock(InviteStore.class);
        Player inviter = player("Inviter");
        Player busy = player("Busy");
        Player member = player("Member");
        Group ownGroup = group();
        Group otherGroup = group();
        VoicechatConnection inviterConnection = connection(ownGroup);
        VoicechatConnection busyConnection = connection(otherGroup);
        VoicechatConnection memberConnection = connection(ownGroup);
        VoicechatServerApi api = mock(VoicechatServerApi.class);
        when(plugin.getVoicechatApi()).thenReturn(api);
        when(api.getConnectionOf(inviter.getUniqueId())).thenReturn(inviterConnection);
        when(api.getConnectionOf(busy.getUniqueId())).thenReturn(busyConnection);
        when(api.getConnectionOf(member.getUniqueId())).thenReturn(memberConnection);
        when(api.getGroup(ownGroup.getId())).thenReturn(ownGroup);
        UUID busyId = busy.getUniqueId();
        UUID memberId = member.getUniqueId();
        UUID ownGroupId = ownGroup.getId();
        when(invites.create(eq(busyId), eq(ownGroupId), any(), any())).thenReturn("token");
        VcGroupCommand command = command(plugin, invites, new GroupLeadershipRegistry());

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getPlayerExact("Busy")).thenReturn(busy);
            bukkit.when(() -> Bukkit.getPlayerExact("Member")).thenReturn(member);

            command.onCommand(inviter, mock(Command.class), "vcgroup", new String[]{"invite", "Busy"});
            command.onCommand(inviter, mock(Command.class), "vcgroup", new String[]{"invite", "Member"});
        }

        verify(invites, times(1)).create(eq(busyId), eq(ownGroupId), any(), any());
        verify(busy, times(1)).sendMessage(any(Component.class));
        verify(invites, never()).create(eq(memberId), eq(ownGroupId), any(), any());
        verify(member, never()).sendMessage(any(Component.class));
    }

    @Test
    void acceptSwitchesGroupsWhenAlreadyInAnotherOne() {
        BetterGroupsPlugin plugin = mock(BetterGroupsPlugin.class);
        UUID playerId = UUID.randomUUID();
        UUID newGroupId = UUID.randomUUID();
        Player player = player("Switcher", playerId);
        Group oldGroup = group();
        Group newGroup = group(newGroupId);
        VoicechatConnection beforeSwitch = connection(oldGroup);
        VoicechatConnection afterSwitch = connection(newGroup);
        VoicechatServerApi api = mock(VoicechatServerApi.class);
        when(plugin.getVoicechatApi()).thenReturn(api);
        when(api.getGroup(newGroupId)).thenReturn(newGroup);
        when(api.getConnectionOf(playerId)).thenReturn(beforeSwitch, afterSwitch);
        InviteStore invites = new InviteStore(
                Clock.fixed(NOW, ZoneOffset.UTC),
                Duration.ofMinutes(5),
                () -> "switch-token"
        );
        invites.create(playerId, newGroupId, UUID.randomUUID(), "Inviter");
        VcGroupCommand command = command(plugin, invites, new GroupLeadershipRegistry());

        command.onCommand(player, mock(Command.class), "vcgroup", new String[]{"accept", "switch-token"});

        verify(beforeSwitch).setGroup(newGroup);
        assertEquals(InviteStore.LookupStatus.NOT_FOUND, invites.lookup("switch-token", playerId).status());
    }

    @Test
    void leaderKickMutatesThenRechecksSnapshot() {
        BetterGroupsPlugin plugin = mock(BetterGroupsPlugin.class);
        Player leader = player("Leader");
        Player target = player("Target");
        Group group = group();
        GroupLeadershipRegistry leadership = new GroupLeadershipRegistry();
        leadership.createGroup(group.getId(), leader.getUniqueId());
        VoicechatConnection leaderConnection = connection(group);
        VoicechatConnection targetBeforeKick = connection(group);
        VoicechatConnection targetAfterKick = connection(null);
        VoicechatServerApi api = mock(VoicechatServerApi.class);
        when(plugin.getVoicechatApi()).thenReturn(api);
        when(api.getConnectionOf(leader.getUniqueId())).thenReturn(leaderConnection);
        when(api.getConnectionOf(target.getUniqueId())).thenReturn(targetBeforeKick, targetAfterKick);
        VcGroupCommand command = command(plugin, mock(InviteStore.class), leadership);

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getPlayerExact("Target")).thenReturn(target);
            command.onCommand(leader, mock(Command.class), "vcgroup", new String[]{"kick", "Target"});
        }

        verify(targetBeforeKick).setGroup(null);
        verify(leader).sendMessage(any(Component.class));
        verify(target).sendMessage(any(Component.class));
    }

    @Test
    void transferCommandPromotesGroupMember() {
        BetterGroupsPlugin plugin = mock(BetterGroupsPlugin.class);
        Player leader = player("Leader");
        Player target = player("Target");
        Group group = group();
        GroupLeadershipRegistry leadership = new GroupLeadershipRegistry();
        leadership.createGroup(group.getId(), leader.getUniqueId());
        leadership.join(group.getId(), target.getUniqueId());
        VoicechatConnection leaderConnection = connection(group);
        VoicechatConnection targetConnection = connection(group);
        VoicechatServerApi api = mock(VoicechatServerApi.class);
        when(plugin.getVoicechatApi()).thenReturn(api);
        when(api.getConnectionOf(leader.getUniqueId())).thenReturn(leaderConnection);
        when(api.getConnectionOf(target.getUniqueId())).thenReturn(targetConnection);
        VcGroupCommand command = command(plugin, mock(InviteStore.class), leadership);

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getPlayerExact("Target")).thenReturn(target);
            command.onCommand(leader, mock(Command.class), "vcgroup", new String[]{"transfer", "Target"});
        }

        assertEquals(
                GroupLeadershipRegistry.Authorization.LEADER,
                leadership.authorize(group.getId(), target.getUniqueId())
        );
        assertEquals(
                GroupLeadershipRegistry.Authorization.NOT_LEADER,
                leadership.authorize(group.getId(), leader.getUniqueId())
        );
        verify(plugin).publishLeadership(any(GroupLeadershipRegistry.Transition.class));
        verify(leader).sendMessage(any(Component.class));
    }

    @Test
    void requestThenLeaderApproveJoinsTheRequester() {
        BetterGroupsPlugin plugin = mock(BetterGroupsPlugin.class);
        Player leader = player("Leader");
        Player requester = player("Requester");
        Group group = group();
        when(group.getName()).thenReturn("Secret");
        when(group.hasPassword()).thenReturn(true);
        when(group.isHidden()).thenReturn(false);
        GroupLeadershipRegistry leadership = new GroupLeadershipRegistry();
        leadership.createGroup(group.getId(), leader.getUniqueId());
        VoicechatConnection requesterBefore = connection(null);
        VoicechatConnection requesterAfter = connection(group);
        VoicechatServerApi api = mock(VoicechatServerApi.class);
        when(plugin.getVoicechatApi()).thenReturn(api);
        when(api.getGroups()).thenReturn(List.of(group));
        when(api.getGroup(group.getId())).thenReturn(group);
        when(api.getConnectionOf(requester.getUniqueId())).thenReturn(
                requesterBefore, requesterBefore, requesterAfter);
        RequestStore requests = new RequestStore(
                Clock.fixed(NOW, ZoneOffset.UTC),
                Duration.ofMinutes(5),
                () -> "req-token"
        );
        VcGroupCommand command = new VcGroupCommand(
                plugin,
                mock(InviteStore.class),
                leadership,
                new InviteCooldownStore(Clock.fixed(NOW, ZoneOffset.UTC), Duration.ZERO),
                requests,
                new InviteCooldownStore(Clock.fixed(NOW, ZoneOffset.UTC), Duration.ZERO),
                new PluginSettings(5, 10, "block.anvil.land", 1.0F, 1.0F, 5, 30, "block.anvil.land", 1.0F, 1.0F)
        );

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getPlayer(leader.getUniqueId())).thenReturn(leader);
            bukkit.when(() -> Bukkit.getPlayer(requester.getUniqueId())).thenReturn(requester);
            when(leader.isOnline()).thenReturn(true);
            when(requester.isOnline()).thenReturn(true);

            command.onCommand(requester, mock(Command.class), "vcgroup", new String[]{"request", "Secret"});
            verify(leader, atLeastOnce()).sendMessage(any(Component.class));
            verify(leader).playSound(
                    any(net.kyori.adventure.sound.Sound.class),
                    any(net.kyori.adventure.sound.Sound.Emitter.class)
            );
            clearInvocations(leader);

            command.onCommand(leader, mock(Command.class), "vcgroup", new String[]{"approve", "req-token"});
        }

        verify(requesterBefore).setGroup(group);
        verify(leader, never()).sendMessage(any(Component.class));
        verify(requester, times(2)).sendMessage(any(Component.class));
        assertEquals(RequestStore.LookupStatus.NOT_FOUND, requests.lookup("req-token").status());
    }

    @Test
    void requestIsRejectedForGroupsWithoutPassword() {
        BetterGroupsPlugin plugin = mock(BetterGroupsPlugin.class);
        Player requester = player("Requester");
        Group group = group();
        when(group.getName()).thenReturn("Open");
        when(group.hasPassword()).thenReturn(false);
        when(group.isHidden()).thenReturn(false);
        VoicechatServerApi api = mock(VoicechatServerApi.class);
        when(plugin.getVoicechatApi()).thenReturn(api);
        when(api.getGroups()).thenReturn(List.of(group));
        VoicechatConnection requesterConnection = connection(null);
        when(api.getConnectionOf(requester.getUniqueId())).thenReturn(requesterConnection);
        RequestStore requests = mock(RequestStore.class);
        VcGroupCommand command = new VcGroupCommand(
                plugin,
                mock(InviteStore.class),
                new GroupLeadershipRegistry(),
                new InviteCooldownStore(Clock.fixed(NOW, ZoneOffset.UTC), Duration.ZERO),
                requests,
                new InviteCooldownStore(Clock.fixed(NOW, ZoneOffset.UTC), Duration.ZERO),
                new PluginSettings(5, 10, "block.anvil.land", 1.0F, 1.0F, 5, 30, "block.anvil.land", 1.0F, 1.0F)
        );

        command.onCommand(requester, mock(Command.class), "vcgroup", new String[]{"request", "Open"});

        verify(requests, never()).create(any(), any());
        verify(requester, times(1)).sendMessage(any(Component.class));
    }

    @Test
    void tabCompletionIsGroupAware() {
        BetterGroupsPlugin plugin = mock(BetterGroupsPlugin.class);
        Player viewer = player("Viewer");
        Player groupmate = player("Groupmate");
        Player outsider = player("Outsider");
        when(viewer.canSee(groupmate)).thenReturn(true);
        when(viewer.canSee(outsider)).thenReturn(true);
        Group ownGroup = group();
        VoicechatConnection viewerConnection = connection(ownGroup);
        VoicechatConnection groupmateConnection = connection(ownGroup);
        VoicechatConnection outsiderConnection = connection(null);
        VoicechatServerApi api = mock(VoicechatServerApi.class);
        when(plugin.getVoicechatApi()).thenReturn(api);
        when(api.getConnectionOf(viewer.getUniqueId())).thenReturn(viewerConnection);
        when(api.getConnectionOf(groupmate.getUniqueId())).thenReturn(groupmateConnection);
        when(api.getConnectionOf(outsider.getUniqueId())).thenReturn(outsiderConnection);
        VcGroupCommand command = command(plugin, mock(InviteStore.class), new GroupLeadershipRegistry());

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getOnlinePlayers).thenReturn(List.of(viewer, groupmate, outsider));

            assertEquals(
                    List.of("Outsider"),
                    command.onTabComplete(viewer, mock(Command.class), "vcgroup", new String[]{"invite", ""})
            );
            assertEquals(
                    List.of("Groupmate"),
                    command.onTabComplete(viewer, mock(Command.class), "vcgroup", new String[]{"kick", ""})
            );
            assertEquals(
                    List.of("Groupmate"),
                    command.onTabComplete(viewer, mock(Command.class), "vcgroup", new String[]{"transfer", ""})
            );
        }
    }

    @Test
    void tabCompletionDoesNotRevealPlayersHiddenFromViewer() {
        Player viewer = player("Viewer");
        Player visible = player("Visible");
        Player hidden = player("Hidden");
        when(viewer.canSee(visible)).thenReturn(true);
        when(viewer.canSee(hidden)).thenReturn(false);
        VcGroupCommand command = command(
                mock(BetterGroupsPlugin.class),
                mock(InviteStore.class),
                new GroupLeadershipRegistry()
        );

        List<String> completions;
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getOnlinePlayers).thenReturn(List.of(visible, hidden));
            completions = command.onTabComplete(
                    viewer,
                    mock(Command.class),
                    "vcgroup",
                    new String[]{"invite", ""}
            );
        }

        assertEquals(List.of("Visible"), completions);
        verify(viewer, never()).sendMessage(any(Component.class));
    }

    private static VcGroupCommand command(
            BetterGroupsPlugin plugin,
            InviteStore invites,
            GroupLeadershipRegistry leadership
    ) {
        return new VcGroupCommand(
                plugin,
                invites,
                leadership,
                new InviteCooldownStore(Clock.fixed(NOW, ZoneOffset.UTC), Duration.ZERO),
                new RequestStore(Clock.fixed(NOW, ZoneOffset.UTC), Duration.ofMinutes(5)),
                new InviteCooldownStore(Clock.fixed(NOW, ZoneOffset.UTC), Duration.ZERO),
                new PluginSettings(5, 10, "block.anvil.land", 1.0F, 1.0F, 5, 30, "block.anvil.land", 1.0F, 1.0F)
        );
    }

    private static Player player(String name) {
        return player(name, UUID.randomUUID());
    }

    private static Player player(String name, UUID playerId) {
        Player player = mock(Player.class);
        when(player.getName()).thenReturn(name);
        when(player.getUniqueId()).thenReturn(playerId);
        when(player.hasPermission("vanillagame.svc_better_groups.use")).thenReturn(true);
        return player;
    }

    private static Group group() {
        return group(UUID.randomUUID());
    }

    private static Group group(UUID groupId) {
        Group group = mock(Group.class);
        when(group.getId()).thenReturn(groupId);
        return group;
    }

    private static VoicechatConnection connection(Group group) {
        VoicechatConnection connection = mock(VoicechatConnection.class);
        when(connection.getGroup()).thenReturn(group);
        return connection;
    }
}
