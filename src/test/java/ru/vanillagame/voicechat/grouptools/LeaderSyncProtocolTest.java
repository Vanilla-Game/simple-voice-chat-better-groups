package ru.vanillagame.voicechat.grouptools;

import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LeaderSyncProtocolTest {

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
