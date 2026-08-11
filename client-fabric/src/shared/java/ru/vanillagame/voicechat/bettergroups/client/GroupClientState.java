package ru.vanillagame.voicechat.bettergroups.client;

import de.maxhenkel.voicechat.voice.client.ClientManager;
import ru.vanillagame.voicechat.bettergroups.client.network.GroupStatePayload;

import java.util.UUID;

public final class GroupClientState {

    private static UUID groupId;
    private static UUID leaderId;

    private GroupClientState() {
    }

    public static void update(GroupStatePayload payload) {
        groupId = payload.groupId();
        leaderId = payload.leaderId();
    }

    public static void clear() {
        groupId = null;
        leaderId = null;
    }

    public static boolean isLeader(UUID playerId) {
        return groupId != null && leaderId != null && leaderId.equals(playerId);
    }

    public static boolean isLocalPlayerLeader() {
        UUID ownId = ClientManager.getPlayerStateManager().getOwnID();
        return ownId != null && isLeader(ownId);
    }
}
