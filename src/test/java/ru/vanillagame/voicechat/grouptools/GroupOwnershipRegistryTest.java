package ru.vanillagame.voicechat.grouptools;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GroupOwnershipRegistryTest {

    @Test
    void onlyRecordedCreatorIsAuthorized() {
        GroupOwnershipRegistry registry = new GroupOwnershipRegistry();
        UUID groupId = UUID.randomUUID();
        UUID creatorId = UUID.randomUUID();
        UUID otherPlayerId = UUID.randomUUID();

        registry.recordCreator(groupId, creatorId);

        assertEquals(
                GroupOwnershipRegistry.Authorization.CREATOR,
                registry.authorize(groupId, creatorId)
        );
        assertEquals(
                GroupOwnershipRegistry.Authorization.NOT_CREATOR,
                registry.authorize(groupId, otherPlayerId)
        );
    }

    @Test
    void unknownOrRemovedOwnershipFailsClosed() {
        GroupOwnershipRegistry registry = new GroupOwnershipRegistry();
        UUID groupId = UUID.randomUUID();
        UUID creatorId = UUID.randomUUID();

        assertEquals(
                GroupOwnershipRegistry.Authorization.UNKNOWN_CREATOR,
                registry.authorize(groupId, creatorId)
        );

        registry.recordCreator(groupId, creatorId);
        registry.remove(groupId);

        assertEquals(
                GroupOwnershipRegistry.Authorization.UNKNOWN_CREATOR,
                registry.authorize(groupId, creatorId)
        );
    }

    @Test
    void duplicateCreateEventCannotReplaceOriginalCreator() {
        GroupOwnershipRegistry registry = new GroupOwnershipRegistry();
        UUID groupId = UUID.randomUUID();
        UUID originalCreator = UUID.randomUUID();
        UUID laterPlayer = UUID.randomUUID();

        registry.recordCreator(groupId, originalCreator);
        registry.recordCreator(groupId, laterPlayer);

        assertEquals(
                GroupOwnershipRegistry.Authorization.CREATOR,
                registry.authorize(groupId, originalCreator)
        );
        assertEquals(
                GroupOwnershipRegistry.Authorization.NOT_CREATOR,
                registry.authorize(groupId, laterPlayer)
        );
    }
}
