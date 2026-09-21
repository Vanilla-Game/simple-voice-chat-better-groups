package ru.vanillagame.voicechat.bettergroups;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class VoiceChatAddonTest {

    @Test
    void duplicateJoinEventDoesNotSendAnotherNotification() {
        BetterGroupsPlugin plugin = mock(BetterGroupsPlugin.class);
        GroupLeadershipRegistry leadership = new GroupLeadershipRegistry();
        VoiceChatAddon addon = new VoiceChatAddon(
                plugin,
                leadership,
                mock(InviteStore.class),
                mock(RequestStore.class)
        );
        UUID groupId = UUID.randomUUID();
        UUID leaderId = UUID.randomUUID();
        UUID joinerId = UUID.randomUUID();
        leadership.createGroup(groupId, leaderId);

        addon.handleGroupJoin(groupId, joinerId);
        addon.handleGroupJoin(groupId, joinerId);

        verify(plugin, times(1)).notifyGroupJoin(groupId, joinerId);
    }

    @Test
    void firstReturnToAnEmptyHeldGroupBecomesLeader() {
        BetterGroupsPlugin plugin = mock(BetterGroupsPlugin.class);
        GroupMuteService pause = mock(GroupMuteService.class);
        org.mockito.Mockito.when(plugin.groupMute()).thenReturn(pause);
        GroupLeadershipRegistry leadership = new GroupLeadershipRegistry();
        UUID group = UUID.randomUUID();
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        org.mockito.Mockito.when(pause.holdsGroup(group)).thenReturn(true);
        VoiceChatAddon addon = new VoiceChatAddon(plugin, leadership, mock(InviteStore.class), mock(RequestStore.class));
        addon.handleGroupJoin(group, first);
        addon.handleGroupJoin(group, second);
        org.junit.jupiter.api.Assertions.assertEquals(first, leadership.leaderOf(group));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    @SuppressWarnings({"unchecked", "rawtypes"})
    void cleanupWaitsForConfirmedNativeRemoval(boolean removed) {
        BetterGroupsPlugin plugin = mock(BetterGroupsPlugin.class);
        GroupMuteService pause = mock(GroupMuteService.class);
        org.mockito.Mockito.when(plugin.groupMute()).thenReturn(pause);
        org.mockito.Mockito.when(plugin.isEnabled()).thenReturn(true);
        var server = mock(org.bukkit.Server.class);
        var scheduler = mock(org.bukkit.scheduler.BukkitScheduler.class);
        org.mockito.Mockito.when(plugin.getServer()).thenReturn(server);
        org.mockito.Mockito.when(server.getScheduler()).thenReturn(scheduler);

        GroupLeadershipRegistry leadership = new GroupLeadershipRegistry();
        InviteStore invites = mock(InviteStore.class);
        RequestStore requests = mock(RequestStore.class);
        VoiceChatAddon addon = new VoiceChatAddon(plugin, leadership, invites, requests);
        var registration = mock(de.maxhenkel.voicechat.api.events.EventRegistration.class);
        addon.registerEvents(registration);
        org.mockito.ArgumentCaptor<java.util.function.Consumer<de.maxhenkel.voicechat.api.events.RemoveGroupEvent>> handler =
                org.mockito.ArgumentCaptor.forClass((Class) java.util.function.Consumer.class);
        verify(registration).registerEvent(org.mockito.ArgumentMatchers.eq(de.maxhenkel.voicechat.api.events.RemoveGroupEvent.class),
                handler.capture(), org.mockito.ArgumentMatchers.eq(-1000));
        UUID groupId = UUID.randomUUID();
        UUID leader = UUID.randomUUID();
        leadership.createGroup(groupId, leader);
        var group = mock(de.maxhenkel.voicechat.api.Group.class);
        org.mockito.Mockito.when(group.getId()).thenReturn(groupId);
        var event = mock(de.maxhenkel.voicechat.api.events.RemoveGroupEvent.class);
        org.mockito.Mockito.when(event.getGroup()).thenReturn(group);
        org.mockito.Mockito.when(pause.holdsGroup(groupId)).thenReturn(true);
        var api = mock(de.maxhenkel.voicechat.api.VoicechatServerApi.class);
        org.mockito.Mockito.when(plugin.getVoicechatApi()).thenReturn(api);
        handler.getValue().accept(event);
        verify(event, org.mockito.Mockito.never()).cancel();
        org.junit.jupiter.api.Assertions.assertEquals(leader, leadership.leaderOf(groupId));
        org.junit.jupiter.api.Assertions.assertEquals(java.util.List.of(leader), leadership.membersOf(groupId));
        org.mockito.Mockito.verifyNoInteractions(invites, requests);
        verify(pause, org.mockito.Mockito.never()).groupRemoved(groupId);

        // A later listener can cancel removal; only the next tick sees its final result.
        org.mockito.Mockito.when(api.getGroup(groupId)).thenReturn(removed ? null : group);
        var task = org.mockito.ArgumentCaptor.forClass(Runnable.class);
        verify(scheduler).runTask(org.mockito.ArgumentMatchers.eq(plugin), task.capture());
        task.getValue().run();
        if (removed) {
            verify(pause).groupRemoved(groupId);
            verify(invites).invalidateGroup(groupId);
            verify(requests).invalidateGroup(groupId);
            org.junit.jupiter.api.Assertions.assertNull(leadership.leaderOf(groupId));
            org.junit.jupiter.api.Assertions.assertTrue(leadership.membersOf(groupId).isEmpty());
        } else {
            verify(pause, org.mockito.Mockito.never()).groupRemoved(groupId);
            org.mockito.Mockito.verifyNoInteractions(invites, requests);
            org.junit.jupiter.api.Assertions.assertEquals(leader, leadership.leaderOf(groupId));
            org.junit.jupiter.api.Assertions.assertEquals(java.util.List.of(leader), leadership.membersOf(groupId));
        }
    }
}
