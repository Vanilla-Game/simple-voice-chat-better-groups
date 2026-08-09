package ru.vanillagame.voicechat.grouptools;

import java.nio.ByteBuffer;
import java.util.UUID;

final class LeaderSyncProtocol {

    static final int VERSION = 1;
    static final String HELLO_CHANNEL = "voicechat_group_tools:hello";
    static final String STATE_CHANNEL = "voicechat_group_tools:leader_state";

    private static final int FLAG_IN_GROUP = 1;
    private static final int FLAG_LEADER_KNOWN = 1 << 1;

    private LeaderSyncProtocol() {
    }

    static boolean isCompatibleHello(byte[] message) {
        return message != null && message.length == 1 && Byte.toUnsignedInt(message[0]) == VERSION;
    }

    static byte[] encode(GroupLeadershipRegistry.PlayerLeadershipState state) {
        int flags = 0;
        int size = 2;
        if (state.inGroup()) {
            flags |= FLAG_IN_GROUP;
            size += Long.BYTES * 2;
        }
        if (state.leaderKnown()) {
            flags |= FLAG_LEADER_KNOWN;
            size += Long.BYTES * 2;
        }

        ByteBuffer buffer = ByteBuffer.allocate(size);
        buffer.put((byte) VERSION);
        buffer.put((byte) flags);
        if (state.inGroup()) {
            putUuid(buffer, state.groupId());
        }
        if (state.leaderKnown()) {
            putUuid(buffer, state.leaderId());
        }
        return buffer.array();
    }

    private static void putUuid(ByteBuffer buffer, UUID uuid) {
        buffer.putLong(uuid.getMostSignificantBits());
        buffer.putLong(uuid.getLeastSignificantBits());
    }
}
