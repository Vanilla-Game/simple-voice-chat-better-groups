package ru.vanillagame.voicechat.grouptools;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

final class GroupOwnershipRegistry {

    private final Map<UUID, UUID> creators = new ConcurrentHashMap<>();

    void recordCreator(UUID groupId, UUID creatorId) {
        creators.putIfAbsent(
                Objects.requireNonNull(groupId, "groupId"),
                Objects.requireNonNull(creatorId, "creatorId")
        );
    }

    Authorization authorize(UUID groupId, UUID playerId) {
        UUID creatorId = creators.get(Objects.requireNonNull(groupId, "groupId"));
        if (creatorId == null) {
            return Authorization.UNKNOWN_CREATOR;
        }
        return creatorId.equals(Objects.requireNonNull(playerId, "playerId"))
                ? Authorization.CREATOR
                : Authorization.NOT_CREATOR;
    }

    void remove(UUID groupId) {
        creators.remove(groupId);
    }

    void clear() {
        creators.clear();
    }

    enum Authorization {
        CREATOR,
        NOT_CREATOR,
        UNKNOWN_CREATOR
    }
}
