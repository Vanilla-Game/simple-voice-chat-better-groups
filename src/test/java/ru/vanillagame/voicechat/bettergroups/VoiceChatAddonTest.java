package ru.vanillagame.voicechat.bettergroups;

import org.junit.jupiter.api.Test;

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

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void removalIsNotPreventedByPausedMembers() {
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
        handler.getValue().accept(event);
        verify(event, org.mockito.Mockito.never()).cancel();
        verify(invites).invalidateGroup(groupId);
        verify(requests).invalidateGroup(groupId);
        org.junit.jupiter.api.Assertions.assertNull(leadership.leaderOf(groupId));
    }
}
