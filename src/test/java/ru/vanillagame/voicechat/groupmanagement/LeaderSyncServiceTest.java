package ru.vanillagame.voicechat.groupmanagement;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.AdditionalMatchers.aryEq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LeaderSyncServiceTest {

    @Test
    void compatibleHelloReturnsServerAuthoritativeState() {
        SvcGroupManagementPlugin plugin = mock(SvcGroupManagementPlugin.class);
        GroupLeadershipRegistry leadership = new GroupLeadershipRegistry();
        LeaderSyncService service = new LeaderSyncService(plugin, leadership);
        Player player = mock(Player.class);
        UUID playerId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(playerId);
        leadership.createGroup(groupId, playerId);

        service.onPluginMessageReceived(
                LeaderSyncProtocol.HELLO_CHANNEL,
                player,
                new byte[]{LeaderSyncProtocol.VERSION}
        );

        verify(player).sendPluginMessage(
                same(plugin),
                eq(LeaderSyncProtocol.STATE_CHANNEL),
                aryEq(LeaderSyncProtocol.encode(leadership.stateFor(playerId)))
        );
    }

    @Test
    void incompatibleHelloDoesNotEnableSync() {
        SvcGroupManagementPlugin plugin = mock(SvcGroupManagementPlugin.class);
        LeaderSyncService service = new LeaderSyncService(plugin, new GroupLeadershipRegistry());
        Player player = mock(Player.class);

        service.onPluginMessageReceived(
                LeaderSyncProtocol.HELLO_CHANNEL,
                player,
                new byte[]{LeaderSyncProtocol.VERSION + 1}
        );

        verify(player, never()).sendPluginMessage(
                same(plugin),
                eq(LeaderSyncProtocol.STATE_CHANNEL),
                any(byte[].class)
        );
    }

    @Test
    void membershipChangeResendsStateToAffectedCompatiblePlayer() {
        SvcGroupManagementPlugin plugin = mock(SvcGroupManagementPlugin.class);
        GroupLeadershipRegistry leadership = new GroupLeadershipRegistry();
        LeaderSyncService service = new LeaderSyncService(plugin, leadership);
        Player member = mock(Player.class);
        UUID leaderId = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();
        when(member.getUniqueId()).thenReturn(memberId);
        when(member.isOnline()).thenReturn(true);
        leadership.createGroup(groupId, leaderId);
        service.onPluginMessageReceived(
                LeaderSyncProtocol.HELLO_CHANNEL,
                member,
                new byte[]{LeaderSyncProtocol.VERSION}
        );
        reset(member);
        when(member.getUniqueId()).thenReturn(memberId);
        when(member.isOnline()).thenReturn(true);

        GroupLeadershipRegistry.Transition transition = leadership.join(groupId, memberId);
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::isPrimaryThread).thenReturn(true);
            bukkit.when(() -> Bukkit.getPlayer(memberId)).thenReturn(member);
            service.publish(transition);
        }

        verify(member).sendPluginMessage(
                same(plugin),
                eq(LeaderSyncProtocol.STATE_CHANNEL),
                aryEq(LeaderSyncProtocol.encode(leadership.stateFor(memberId)))
        );
    }
}
