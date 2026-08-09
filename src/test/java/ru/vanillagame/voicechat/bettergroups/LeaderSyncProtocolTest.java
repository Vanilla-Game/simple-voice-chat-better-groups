package ru.vanillagame.voicechat.bettergroups;

import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.util.HexFormat;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LeaderSyncProtocolTest {

    // Golden vectors: the version 1 wire format shared with the Fabric client
    // payload codecs (HelloPayload, LeaderStatePayload). Deployed clients parse
    // exactly these bytes, so a change that breaks these assertions breaks
    // players in the wild — never edit the vectors; bump
    // LeaderSyncProtocol.VERSION and add new vectors instead.
    @Test
    void goldenVectorsPinTheVersion1WireFormat() {
        UUID groupId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID leaderId = UUID.fromString("00000000-0000-0000-0000-000000000002");

        assertTrue(LeaderSyncProtocol.isCompatibleHello(HexFormat.of().parseHex("01")));
        assertArrayEquals(
                HexFormat.of().parseHex("0100"),
                LeaderSyncProtocol.encode(GroupLeadershipRegistry.PlayerLeadershipState.notInGroup())
        );
        assertArrayEquals(
                HexFormat.of().parseHex("0101" + "00000000000000000000000000000001"),
                LeaderSyncProtocol.encode(
                        new GroupLeadershipRegistry.PlayerLeadershipState(groupId, null))
        );
        assertArrayEquals(
                HexFormat.of().parseHex(
                        "0103"
                                + "00000000000000000000000000000001"
                                + "00000000000000000000000000000002"),
                LeaderSyncProtocol.encode(
                        new GroupLeadershipRegistry.PlayerLeadershipState(groupId, leaderId))
        );
    }

    @Test
    void helloRequiresExactlyOneSupportedVersionByte() {
        assertTrue(LeaderSyncProtocol.isCompatibleHello(new byte[]{1}));
        assertFalse(LeaderSyncProtocol.isCompatibleHello(new byte[]{}));
        assertFalse(LeaderSyncProtocol.isCompatibleHello(new byte[]{2}));
        assertFalse(LeaderSyncProtocol.isCompatibleHello(new byte[]{1, 0}));
        assertFalse(LeaderSyncProtocol.isCompatibleHello(null));
    }

    @Test
    void stateEncodingIncludesVersionFlagsGroupAndLeader() {
        UUID groupId = UUID.randomUUID();
        UUID leaderId = UUID.randomUUID();
        ByteBuffer expected = ByteBuffer.allocate(34)
                .put((byte) 1)
                .put((byte) 3)
                .putLong(groupId.getMostSignificantBits())
                .putLong(groupId.getLeastSignificantBits())
                .putLong(leaderId.getMostSignificantBits())
                .putLong(leaderId.getLeastSignificantBits());

        assertArrayEquals(
                expected.array(),
                LeaderSyncProtocol.encode(
                        new GroupLeadershipRegistry.PlayerLeadershipState(groupId, leaderId)
                )
        );
    }

    @Test
    void unknownLeaderAndNoGroupHaveDistinctStates() {
        UUID groupId = UUID.randomUUID();

        assertArrayEquals(
                ByteBuffer.allocate(18)
                        .put((byte) 1)
                        .put((byte) 1)
                        .putLong(groupId.getMostSignificantBits())
                        .putLong(groupId.getLeastSignificantBits())
                        .array(),
                LeaderSyncProtocol.encode(
                        new GroupLeadershipRegistry.PlayerLeadershipState(groupId, null)
                )
        );
        assertArrayEquals(
                new byte[]{1, 0},
                LeaderSyncProtocol.encode(GroupLeadershipRegistry.PlayerLeadershipState.notInGroup())
        );
    }
}
