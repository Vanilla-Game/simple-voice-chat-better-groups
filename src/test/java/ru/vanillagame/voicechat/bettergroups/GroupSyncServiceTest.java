package ru.vanillagame.voicechat.bettergroups;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.MockedStatic;

import java.util.UUID;

import static org.mockito.AdditionalMatchers.aryEq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GroupSyncServiceTest {

    @Test
    void compatibleHelloNegotiatesWithoutSendingEmptyGroupState() {
        BetterGroupsPlugin plugin = mock(BetterGroupsPlugin.class);
        GroupSyncService service = new GroupSyncService(plugin, new GroupLeadershipRegistry());
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());

        service.onPluginMessageReceived(
                GroupSyncProtocol.CLIENT_HELLO_CHANNEL,
                player,
                new byte[]{GroupSyncProtocol.VERSION}
        );

        verify(player).sendPluginMessage(
                same(plugin),
                eq(GroupSyncProtocol.SERVER_HELLO_CHANNEL),
                aryEq(GroupSyncProtocol.encodeServerHello())
        );
        verify(player, never()).sendPluginMessage(
                same(plugin),
                eq(GroupSyncProtocol.GROUP_STATE_CHANNEL),
                any(byte[].class)
        );
    }

    @Test
    void compatibleHelloSendsCurrentGroupStateWhenPlayerIsInAGroup() {
        BetterGroupsPlugin plugin = mock(BetterGroupsPlugin.class);
        GroupLeadershipRegistry leadership = new GroupLeadershipRegistry();
        GroupSyncService service = new GroupSyncService(plugin, leadership);
        Player player = mock(Player.class);
        UUID playerId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(playerId);
        leadership.createGroup(groupId, playerId);

        service.onPluginMessageReceived(
                GroupSyncProtocol.CLIENT_HELLO_CHANNEL,
                player,
                new byte[]{GroupSyncProtocol.VERSION}
        );

        InOrder messages = inOrder(player);
        messages.verify(player).sendPluginMessage(
                same(plugin),
                eq(GroupSyncProtocol.SERVER_HELLO_CHANNEL),
                aryEq(GroupSyncProtocol.encodeServerHello())
        );
        messages.verify(player).sendPluginMessage(
                same(plugin),
                eq(GroupSyncProtocol.GROUP_STATE_CHANNEL),
                aryEq(GroupSyncProtocol.encodeGroupState(leadership.stateFor(playerId)))
        );
    }

    @Test
    void incompatibleHelloDoesNotEnableSync() {
        BetterGroupsPlugin plugin = mock(BetterGroupsPlugin.class);
        GroupSyncService service = new GroupSyncService(plugin, new GroupLeadershipRegistry());
        Player player = mock(Player.class);

        service.onPluginMessageReceived(
                GroupSyncProtocol.CLIENT_HELLO_CHANNEL,
                player,
                new byte[]{GroupSyncProtocol.VERSION + 1}
        );

        verify(player, never()).sendPluginMessage(
                same(plugin),
                eq(GroupSyncProtocol.SERVER_HELLO_CHANNEL),
                any(byte[].class)
        );
        verify(player, never()).sendPluginMessage(
                same(plugin),
                eq(GroupSyncProtocol.GROUP_STATE_CHANNEL),
                any(byte[].class)
        );
    }

    @Test
    void membershipChangeSendsGroupStateToAffectedCompatiblePlayer() {
        BetterGroupsPlugin plugin = mock(BetterGroupsPlugin.class);
        GroupLeadershipRegistry leadership = new GroupLeadershipRegistry();
        GroupSyncService service = new GroupSyncService(plugin, leadership);
        Player member = mock(Player.class);
        UUID leaderId = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();
        when(member.getUniqueId()).thenReturn(memberId);
        when(member.isOnline()).thenReturn(true);
        leadership.createGroup(groupId, leaderId);
        service.onPluginMessageReceived(
                GroupSyncProtocol.CLIENT_HELLO_CHANNEL,
                member,
                new byte[]{GroupSyncProtocol.VERSION}
        );
        reset(member);
        when(member.getUniqueId()).thenReturn(memberId);
        when(member.isOnline()).thenReturn(true);

        GroupLeadershipRegistry.Transition transition = leadership.join(groupId, memberId);
        publishOnMainThread(service, transition, memberId, member);

        verify(member).sendPluginMessage(
                same(plugin),
                eq(GroupSyncProtocol.GROUP_STATE_CHANNEL),
                aryEq(GroupSyncProtocol.encodeGroupState(leadership.stateFor(memberId)))
        );
    }

    @Test
    void leavingGroupSendsEmptyGroupStateToClearClientCache() {
        BetterGroupsPlugin plugin = mock(BetterGroupsPlugin.class);
        GroupLeadershipRegistry leadership = new GroupLeadershipRegistry();
        GroupSyncService service = new GroupSyncService(plugin, leadership);
        Player member = mock(Player.class);
        UUID leaderId = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();
        leadership.createGroup(groupId, leaderId);
        leadership.join(groupId, memberId);
        when(member.getUniqueId()).thenReturn(memberId);
        when(member.isOnline()).thenReturn(true);
        service.onPluginMessageReceived(
                GroupSyncProtocol.CLIENT_HELLO_CHANNEL,
                member,
                new byte[]{GroupSyncProtocol.VERSION}
        );
        reset(member);
        when(member.getUniqueId()).thenReturn(memberId);
        when(member.isOnline()).thenReturn(true);

        GroupLeadershipRegistry.Transition transition = leadership.leave(groupId, memberId);
        publishOnMainThread(service, transition, memberId, member);

        verify(member).sendPluginMessage(
                same(plugin),
                eq(GroupSyncProtocol.GROUP_STATE_CHANNEL),
                aryEq(GroupSyncProtocol.encodeGroupState(
                        GroupLeadershipRegistry.PlayerLeadershipState.notInGroup()))
        );
    }

    private static void publishOnMainThread(
            GroupSyncService service,
            GroupLeadershipRegistry.Transition transition,
            UUID playerId,
            Player player
    ) {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::isPrimaryThread).thenReturn(true);
            bukkit.when(() -> Bukkit.getPlayer(playerId)).thenReturn(player);
            service.publish(transition);
        }
    }
}
