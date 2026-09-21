package ru.vanillagame.voicechat.bettergroups;

import de.maxhenkel.voicechat.api.Group;
import de.maxhenkel.voicechat.api.VoicechatConnection;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.config.ConfigAccessor;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitScheduler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class GroupMuteServiceTest {
    private final BetterGroupsPlugin plugin = mock(BetterGroupsPlugin.class);
    private final VoicechatServerApi api = mock(VoicechatServerApi.class);
    private final GroupLeadershipRegistry leadership = new GroupLeadershipRegistry();
    private final GroupMuteService service = new GroupMuteService(plugin, leadership);
    private final UUID id = UUID.randomUUID();
    private final UUID groupId = UUID.randomUUID();
    private final Group group = mock(Group.class);
    private final Player player = mock(Player.class);
    private final AtomicReference<Group> membership = new AtomicReference<>();
    private final List<Runnable> scheduled = new ArrayList<>();
    private boolean connected = true;
    private boolean removeEmptyGroup;
    private boolean cancelLeave;
    private boolean cancelJoin;
    private int leaves;
    private int joins;
    private final ConfigAccessor config = mock(ConfigAccessor.class);

    @BeforeEach void setup() {
        when(plugin.getVoicechatApi()).thenReturn(api);
        when(player.getUniqueId()).thenReturn(id);
        when(player.hasPermission("voicechat.groups")).thenReturn(true);
        when(group.getId()).thenReturn(groupId);
        when(group.getType()).thenReturn(Group.Type.OPEN);
        when(group.hasPassword()).thenReturn(true);
        when(api.getGroup(groupId)).thenReturn(group);
        when(api.getServerConfig()).thenReturn(config);
        when(config.getBoolean("enable_groups", true)).thenReturn(true);
        membership.set(group);
        // Match SVC: a connection is a snapshot, not a live mutable view.
        when(api.getConnectionOf(id)).thenAnswer(invocation -> {
            var snapshot = mock(VoicechatConnection.class);
            when(snapshot.getGroup()).thenReturn(membership.get());
            when(snapshot.isConnected()).thenReturn(connected);
            doAnswer(change -> {
                Group next = change.getArgument(0);
                if (next == null) {
                    leaves++;
                    if (cancelLeave) return null;
                } else {
                    joins++;
                    if (cancelJoin) return null;
                }
                service.membershipChanged(id);
                membership.set(next);
                if (next == null) leadership.leave(groupId, id);
                else leadership.join(next.getId(), id);
                // SVC tries cleanup immediately after leave, before returning from setGroup.
                if (next == null && removeEmptyGroup) when(api.getGroup(groupId)).thenReturn(null);
                return null;
            }).when(snapshot).setGroup(any());
            return snapshot;
        });
        var server = mock(Server.class);
        var scheduler = mock(BukkitScheduler.class);
        when(plugin.getServer()).thenReturn(server);
        when(server.getScheduler()).thenReturn(scheduler);
        doAnswer(call -> { scheduled.add(call.getArgument(1)); return null; })
                .when(scheduler).runTask(eq(plugin), any(Runnable.class));
        hello();
    }

    private void hello() {
        service.onPluginMessageReceived(GroupMuteProtocol.REQUEST, player, new byte[]{1,0,0,0,0,0,0});
    }

    private void request(boolean paused) { request(groupId, paused); }

    private void request(UUID target, boolean paused) {
        service.onPluginMessageReceived(GroupMuteProtocol.REQUEST, player,
                ByteBuffer.allocate(23).put((byte) 1).putInt(1).put((byte) 1)
                        .putLong(target.getMostSignificantBits()).putLong(target.getLeastSignificantBits())
                        .put((byte) (paused ? 1 : 0)).array());
    }

    private void assertState(int result, UUID destination) {
        var bytes = ArgumentCaptor.forClass(byte[].class);
        verify(player, atLeastOnce()).sendPluginMessage(eq(plugin), eq(GroupMuteProtocol.STATE), bytes.capture());
        assertArrayEquals(GroupMuteProtocol.encode(1, result, destination), bytes.getValue());
    }

    @Test void leavesAndReturnsToPasswordProtectedGroupUsingServerGrant() {
        request(true);
        assertNull(membership.get());
        assertTrue(service.holdsGroup(groupId));
        assertState(GroupMuteProtocol.OK, groupId);
        request(false);
        assertSame(group, membership.get());
        assertFalse(service.holdsGroup(groupId));
        assertState(GroupMuteProtocol.OK, null);
        assertEquals(1, leaves);
        assertEquals(1, joins);
    }

    @Test void lastParticipantLeavesWithoutReturnGrantAndRetryDoesNotRecreateGroup() {
        removeEmptyGroup = true;
        request(true);
        assertNull(membership.get());
        assertFalse(service.holdsGroup(groupId));
        assertState(GroupMuteProtocol.OK, null);
        request(true);
        assertState(GroupMuteProtocol.OK, null);
        request(false);
        assertState(GroupMuteProtocol.REJECTED, null);
        assertEquals(1, leaves);
        assertEquals(0, joins);
    }

    @Test void removalRevokesPreviouslyPausedPlayersReturnGrants() {
        request(true);
        service.groupRemoved(groupId);
        assertFalse(service.holdsGroup(groupId));
        request(false);
        assertState(GroupMuteProtocol.REJECTED, null);
        assertEquals(0, joins);
    }

    @Test void returningLeaderReclaimsLeadershipOnlyAfterSuccessfulJoin() {
        UUID other = UUID.randomUUID();
        leadership.createGroup(groupId, id);
        leadership.join(groupId, other);
        request(true);
        assertEquals(other, leadership.leaderOf(groupId));
        cancelJoin = true;
        request(false);
        assertEquals(other, leadership.leaderOf(groupId));
        cancelJoin = false;
        request(false);
        assertEquals(id, leadership.leaderOf(groupId));
        request(false);
        assertEquals(id, leadership.leaderOf(groupId));
    }

    @Test void returningMemberDoesNotTakeLeadership() {
        UUID other = UUID.randomUUID();
        leadership.createGroup(groupId, other);
        leadership.join(groupId, id);
        request(true); request(false);
        assertEquals(other, leadership.leaderOf(groupId));
    }

    @Test void manualGroupChangeRevokesSavedLeaderRole() {
        UUID other = UUID.randomUUID();
        leadership.createGroup(groupId, id);
        leadership.join(groupId, other);
        request(true);
        service.membershipChanged(id);
        request(false);
        assertEquals(other, leadership.leaderOf(groupId));
        assertEquals(0, joins);
    }

    @Test void supportsEveryGroupTypeThroughNativeLeave() {
        for (Group.Type type : new Group.Type[]{Group.Type.OPEN, Group.Type.NORMAL, Group.Type.ISOLATED}) {
            when(group.getType()).thenReturn(type);
            request(true);
            assertNull(membership.get());
            request(false);
            assertSame(group, membership.get());
        }
    }

    @Test void repeatedRequestsAreIdempotent() {
        request(true); request(true);
        assertEquals(1, leaves);
        assertState(GroupMuteProtocol.OK, groupId);
        request(false); request(false);
        assertEquals(1, joins);
        assertState(GroupMuteProtocol.OK, null);
    }

    @Test void refusesPasswordBypassWithoutPriorLeave() {
        membership.set(null);
        request(false);
        assertEquals(0, joins);
        assertState(GroupMuteProtocol.REJECTED, null);
    }

    @Test void returnGrantCannotBeUsedForAnotherGroup() {
        request(true);
        request(UUID.randomUUID(), false);
        assertEquals(0, joins);
        assertState(GroupMuteProtocol.REJECTED, groupId);
    }

    @Test void cancelledLeaveDoesNotReportSuccessOrHoldGroup() {
        cancelLeave = true;
        request(true);
        assertSame(group, membership.get());
        assertFalse(service.holdsGroup(groupId));
        assertState(GroupMuteProtocol.REJECTED, null);
    }

    @Test void cancelledReturnKeepsTheGrantForRetry() {
        request(true);
        cancelJoin = true;
        request(false);
        assertNull(membership.get());
        assertTrue(service.holdsGroup(groupId));
        assertState(GroupMuteProtocol.REJECTED, groupId);
        cancelJoin = false;
        request(false);
        assertSame(group, membership.get());
    }

    @Test void removedGroupIsNotRecreated() {
        request(true);
        when(api.getGroup(groupId)).thenReturn(null);
        request(false);
        assertEquals(0, joins);
        assertFalse(service.holdsGroup(groupId));
        assertState(GroupMuteProtocol.REJECTED, null);
    }

    @Test void manualJoinRevokesGrantAndDefersCleanupUntilMembershipCommits() {
        request(true);
        service.membershipChanged(id);
        assertFalse(service.holdsGroup(groupId));
        verify(api, never()).removeGroup(any());
        assertEquals(1, scheduled.size());
        membership.set(group);
        scheduled.forEach(Runnable::run);
        verify(api).removeGroup(groupId); // SVC now sees the occupant and refuses removal.
        membership.set(null);
        request(false);
        assertEquals(0, joins);
        assertState(GroupMuteProtocol.REJECTED, null);
    }

    @Test void disconnectReleasesTheEmptyGroupAndRevokesReturn() {
        request(true);
        service.forget(id);
        assertFalse(service.holdsGroup(groupId));
        verify(api).removeGroup(groupId);
        hello(); request(false);
        assertEquals(0, joins);
        assertState(GroupMuteProtocol.REJECTED, null);
    }

    @Test void persistentGroupsAreNeverCleanedUpByPause() {
        when(group.isPersistent()).thenReturn(true);
        request(true); service.clearPlayer(id);
        verify(api, never()).removeGroup(any());
    }

    @Test void serverShutdownReleasesAllGrants() {
        request(true); service.clear();
        assertFalse(service.holdsGroup(groupId));
        verify(api).removeGroup(groupId);
    }

    @Test void permissionOrDisabledGroupsPreventsReturnButAllowsRetry() {
        request(true);
        when(player.hasPermission("voicechat.groups")).thenReturn(false);
        request(false);
        assertEquals(0, joins);
        assertState(GroupMuteProtocol.REJECTED, groupId);
        when(player.hasPermission("voicechat.groups")).thenReturn(true);
        when(config.getBoolean("enable_groups", true)).thenReturn(false);
        request(false);
        assertEquals(0, joins);
        assertState(GroupMuteProtocol.REJECTED, groupId);
    }

    @Test void leavesAndReturnsWithoutVoiceUdpConnection() {
        connected = false;
        leavesAndReturnsToPasswordProtectedGroupUsingServerGrant();
    }

    @Test void requiresHandshake() {
        service.forget(id); request(true);
        assertEquals(0, leaves);
    }

    @Test void staleRequestDoesNotSwitchThePlayersNewGroup() {
        Group other = mock(Group.class);
        when(other.getId()).thenReturn(UUID.randomUUID());
        membership.set(other);
        request(true); request(false);
        assertEquals(0, leaves);
        assertEquals(0, joins);
        assertSame(other, membership.get());
    }

    @Test void retentionLastsUntilEveryPausedMemberHasReleasedIt() {
        request(true);
        UUID other = UUID.randomUUID();
        Player otherPlayer = mock(Player.class);
        when(otherPlayer.getUniqueId()).thenReturn(other);
        VoicechatConnection otherConnection = mock(VoicechatConnection.class);
        when(otherConnection.isConnected()).thenReturn(true);
        when(otherConnection.getGroup()).thenReturn(group);
        when(api.getConnectionOf(other)).thenReturn(otherConnection);
        doAnswer(call -> { when(otherConnection.getGroup()).thenReturn(null); return null; })
                .when(otherConnection).setGroup(null);
        service.onPluginMessageReceived(GroupMuteProtocol.REQUEST, otherPlayer, new byte[]{1,0,0,0,0,0,0});
        byte[] pause = ByteBuffer.allocate(23).put((byte) 1).putInt(1).put((byte) 1)
                .putLong(groupId.getMostSignificantBits()).putLong(groupId.getLeastSignificantBits()).put((byte) 1).array();
        service.onPluginMessageReceived(GroupMuteProtocol.REQUEST, otherPlayer, pause);
        service.clearPlayer(id);
        assertTrue(service.holdsGroup(groupId));
        verify(api, never()).removeGroup(any());
        service.clearPlayer(other);
        assertFalse(service.holdsGroup(groupId));
        verify(api).removeGroup(groupId);
    }
}
