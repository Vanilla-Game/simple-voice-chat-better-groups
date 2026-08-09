package ru.vanillagame.voicechat.grouptools.client;

import de.maxhenkel.voicechat.voice.client.ClientManager;
import ru.vanillagame.voicechat.grouptools.client.network.LeaderStatePayload;

import java.util.UUID;

public final class LeaderClientState {

    private static UUID groupId;
    private static UUID leaderId;

    private LeaderClientState() {
    }

    public static void update(LeaderStatePayload payload) {
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
