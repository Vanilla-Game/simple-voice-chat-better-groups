package ru.vanillagame.voicechat.grouptools;

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
        VoiceChatGroupToolsPlugin plugin = mock(VoiceChatGroupToolsPlugin.class);
        InviteStore invites = mock(InviteStore.class);
        GroupOwnershipRegistry ownership = new GroupOwnershipRegistry();
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
        when(invites.create(target.getUniqueId(), group.getId())).thenReturn("token");
        when(invites.create(other.getUniqueId(), group.getId())).thenReturn("token2");
        VcGroupCommand command = new VcGroupCommand(plugin, invites, ownership, cooldowns, 5);

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getPlayerExact("Target")).thenReturn(target);
            bukkit.when(() -> Bukkit.getPlayerExact("Other")).thenReturn(other);

            command.onCommand(inviter, mock(Command.class), "vcgroup", new String[]{"invite", "Target"});
            command.onCommand(inviter, mock(Command.class), "vcgroup", new String[]{"invite", "Target"});
            command.onCommand(inviter, mock(Command.class), "vcgroup", new String[]{"invite", "Other"});
        }

        verify(invites, times(1)).create(target.getUniqueId(), group.getId());
        verify(target, times(1)).sendMessage(any(Component.class));
        verify(invites, times(1)).create(other.getUniqueId(), group.getId());
        verify(other, times(1)).sendMessage(any(Component.class));
    }

    @Test
    void acceptMutatesThenRechecksSnapshotAndConsumesInvite() {
        VoiceChatGroupToolsPlugin plugin = mock(VoiceChatGroupToolsPlugin.class);
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
        invites.create(playerId, groupId);
        VcGroupCommand command = command(plugin, invites, new GroupOwnershipRegistry());

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
    void creatorKickMutatesThenRechecksSnapshot() {
        VoiceChatGroupToolsPlugin plugin = mock(VoiceChatGroupToolsPlugin.class);
        Player creator = player("Creator");
        Player target = player("Target");
        Group group = group();
        GroupOwnershipRegistry ownership = new GroupOwnershipRegistry();
        ownership.recordCreator(group.getId(), creator.getUniqueId());
        VoicechatConnection creatorConnection = connection(group);
        VoicechatConnection targetBeforeKick = connection(group);
        VoicechatConnection targetAfterKick = connection(null);
        VoicechatServerApi api = mock(VoicechatServerApi.class);
        when(plugin.getVoicechatApi()).thenReturn(api);
        when(api.getConnectionOf(creator.getUniqueId())).thenReturn(creatorConnection);
        when(api.getConnectionOf(target.getUniqueId())).thenReturn(targetBeforeKick, targetAfterKick);
        VcGroupCommand command = command(plugin, mock(InviteStore.class), ownership);

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getPlayerExact("Target")).thenReturn(target);
            command.onCommand(creator, mock(Command.class), "vcgroup", new String[]{"kick", "Target"});
        }

        verify(targetBeforeKick).setGroup(null);
        verify(creator).sendMessage(any(Component.class));
        verify(target).sendMessage(any(Component.class));
    }

    @Test
    void tabCompletionDoesNotRevealPlayersHiddenFromViewer() {
        Player viewer = player("Viewer");
        Player visible = player("Visible");
        Player hidden = player("Hidden");
        when(viewer.canSee(visible)).thenReturn(true);
        when(viewer.canSee(hidden)).thenReturn(false);
        VcGroupCommand command = command(
                mock(VoiceChatGroupToolsPlugin.class),
                mock(InviteStore.class),
                new GroupOwnershipRegistry()
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
            VoiceChatGroupToolsPlugin plugin,
            InviteStore invites,
            GroupOwnershipRegistry ownership
    ) {
        return new VcGroupCommand(
                plugin,
                invites,
                ownership,
                new InviteCooldownStore(Clock.fixed(NOW, ZoneOffset.UTC), Duration.ZERO),
                5
        );
    }

    private static Player player(String name) {
        return player(name, UUID.randomUUID());
    }

    private static Player player(String name, UUID playerId) {
        Player player = mock(Player.class);
        when(player.getName()).thenReturn(name);
        when(player.getUniqueId()).thenReturn(playerId);
        when(player.hasPermission("vanillagame.voicechat_group_tools.use")).thenReturn(true);
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
