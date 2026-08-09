package ru.vanillagame.voicechat.bettergroups;

import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.util.HexFormat;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GroupSyncProtocolTest {

    // Golden vectors: the version 2 wire format shared with the Fabric client
    // payload codecs. Deployed clients parse exactly these bytes, so a wire
    // change requires a new GroupSyncProtocol.VERSION and new vectors.
    @Test
    void goldenVectorsPinTheVersion2WireFormat() {
        UUID groupId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID leaderId = UUID.fromString("00000000-0000-0000-0000-000000000002");

        assertTrue(GroupSyncProtocol.isCompatibleClientHello(HexFormat.of().parseHex("02")));
        assertArrayEquals(HexFormat.of().parseHex("02"), GroupSyncProtocol.encodeServerHello());
        assertArrayEquals(
                HexFormat.of().parseHex("0200"),
                GroupSyncProtocol.encodeGroupState(
                        GroupLeadershipRegistry.PlayerLeadershipState.notInGroup())
        );
        assertArrayEquals(
                HexFormat.of().parseHex("0201" + "00000000000000000000000000000001"),
                GroupSyncProtocol.encodeGroupState(
                        new GroupLeadershipRegistry.PlayerLeadershipState(groupId, null))
        );
        assertArrayEquals(
                HexFormat.of().parseHex(
                        "0203"
                                + "00000000000000000000000000000001"
                                + "00000000000000000000000000000002"),
                GroupSyncProtocol.encodeGroupState(
                        new GroupLeadershipRegistry.PlayerLeadershipState(groupId, leaderId))
        );
    }

    @Test
    void clientHelloRequiresExactlyOneSupportedVersionByte() {
        assertTrue(GroupSyncProtocol.isCompatibleClientHello(new byte[]{2}));
        assertFalse(GroupSyncProtocol.isCompatibleClientHello(new byte[]{}));
        assertFalse(GroupSyncProtocol.isCompatibleClientHello(new byte[]{1}));
        assertFalse(GroupSyncProtocol.isCompatibleClientHello(new byte[]{2, 0}));
        assertFalse(GroupSyncProtocol.isCompatibleClientHello(null));
    }

    @Test
    void groupStateEncodingIncludesVersionFlagsGroupAndLeader() {
        UUID groupId = UUID.randomUUID();
        UUID leaderId = UUID.randomUUID();
        ByteBuffer expected = ByteBuffer.allocate(34)
                .put((byte) 2)
                .put((byte) 3)
                .putLong(groupId.getMostSignificantBits())
                .putLong(groupId.getLeastSignificantBits())
                .putLong(leaderId.getMostSignificantBits())
                .putLong(leaderId.getLeastSignificantBits());

        assertArrayEquals(
                expected.array(),
                GroupSyncProtocol.encodeGroupState(
                        new GroupLeadershipRegistry.PlayerLeadershipState(groupId, leaderId)
                )
        );
    }

    @Test
    void unknownLeaderAndNoGroupHaveDistinctStates() {
        UUID groupId = UUID.randomUUID();

        assertArrayEquals(
                ByteBuffer.allocate(18)
                        .put((byte) 2)
                        .put((byte) 1)
                        .putLong(groupId.getMostSignificantBits())
                        .putLong(groupId.getLeastSignificantBits())
                        .array(),
                GroupSyncProtocol.encodeGroupState(
                        new GroupLeadershipRegistry.PlayerLeadershipState(groupId, null)
                )
        );
        assertArrayEquals(
                new byte[]{2, 0},
                GroupSyncProtocol.encodeGroupState(
                        GroupLeadershipRegistry.PlayerLeadershipState.notInGroup())
        );
    }
}
