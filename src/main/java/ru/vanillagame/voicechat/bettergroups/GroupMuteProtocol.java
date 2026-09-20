package ru.vanillagame.voicechat.bettergroups;

import java.nio.ByteBuffer;
import java.util.UUID;

// Independent versioned extension; the existing v2 leadership protocol stays unchanged.
// request: version u8, request i32 (0 = hello), group-present u8, optional UUID, muted u8
// state: version u8, request i32 (-1 = unsolicited), result u8, paused-group-present u8,
//        optional UUID. Integers are big-endian. Separate channels from the old audio-mute experiment.
final class GroupMuteProtocol {
    static final int VERSION = 1;
    static final String REQUEST = "svc_better_groups:pause_request";
    static final String STATE = "svc_better_groups:pause_state";
    static final int OK = 0;
    static final int REJECTED = 1;

    record Request(int id, UUID group, boolean muted) {}

    static Request decode(byte[] bytes) {
        if (bytes == null || (bytes.length != 7 && bytes.length != 23)) return null;
        ByteBuffer b = ByteBuffer.wrap(bytes);
        if (Byte.toUnsignedInt(b.get()) != VERSION) return null;
        int id = b.getInt();
        int present = Byte.toUnsignedInt(b.get());
        if (present > 1 || bytes.length != (present == 1 ? 23 : 7)) return null;
        UUID group = present == 1 ? new UUID(b.getLong(), b.getLong()) : null;
        int muted = Byte.toUnsignedInt(b.get());
        if (muted > 1 || id < 0 || (id == 0 && (group != null || muted != 0))
                || (id > 0 && group == null)) return null;
        return new Request(id, group, muted == 1);
    }

    static byte[] encode(int request, int result, UUID group) {
        ByteBuffer b = ByteBuffer.allocate(7 + (group == null ? 0 : 16));
        b.put((byte) VERSION).putInt(request).put((byte) result).put((byte) (group == null ? 0 : 1));
        if (group != null) putUuid(b, group);
        return b.array();
    }

    private static void putUuid(ByteBuffer b, UUID id) {
        b.putLong(id.getMostSignificantBits()).putLong(id.getLeastSignificantBits());
    }
}
