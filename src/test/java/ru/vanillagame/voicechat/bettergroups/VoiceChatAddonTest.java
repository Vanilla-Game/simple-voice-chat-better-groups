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
}
