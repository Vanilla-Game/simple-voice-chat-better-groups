package ru.vanillagame.voicechat.bettergroups;

import org.junit.jupiter.api.Test;
import java.util.HexFormat;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class GroupMuteProtocolTest {
    private static final UUID GROUP = UUID.fromString("00112233-4455-6677-8899-aabbccddeeff");

    @Test void requestGoldenVector() {
        var request = GroupMuteProtocol.decode(HexFormat.of().parseHex("010000002a0100112233445566778899aabbccddeeff01"));
        assertEquals(new GroupMuteProtocol.Request(42, GROUP, true), request);
        assertEquals(new GroupMuteProtocol.Request(0, null, false), GroupMuteProtocol.decode(new byte[]{1,0,0,0,0,0,0}));
    }

    @Test void stateGoldenVector() {
        assertEquals("010000002a000100112233445566778899aabbccddeeff",
                HexFormat.of().formatHex(GroupMuteProtocol.encode(42, 0, GROUP)));
        assertEquals("01ffffffff0000", HexFormat.of().formatHex(GroupMuteProtocol.encode(-1, 0, null)));
    }

    @Test void rejectsMalformedRequests() {
        for (String bytes : new String[]{"", "010000000000", "02000000000000", "01000000000200", "01000000000002",
                "01ffffffff0000", "01000000010000", "01000000000001", "01000000000100112233445566778899aabbccddeeff00"}) {
            assertNull(GroupMuteProtocol.decode(HexFormat.of().parseHex(bytes)), bytes);
        }
        assertNull(GroupMuteProtocol.decode(null));
    }
}
