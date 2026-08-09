package ru.vanillagame.voicechat.grouptools;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

final class GroupLeadershipRegistry {

    private final Map<UUID, GroupState> groups = new HashMap<>();
    private final Map<UUID, UUID> playerGroups = new HashMap<>();

    synchronized Transition createGroup(UUID groupId, UUID creatorId) {
        Objects.requireNonNull(groupId, "groupId");
        Objects.requireNonNull(creatorId, "creatorId");

        TransitionBuilder transition = new TransitionBuilder();
        detachFromPreviousGroup(groupId, creatorId, transition);

        GroupState existing = groups.get(groupId);
        if (existing != null && existing.leaderId != null) {
            if (existing.leaderId.equals(creatorId) && existing.members.add(creatorId)) {
                playerGroups.put(creatorId, groupId);
                transition.affect(creatorId);
            }
            return transition.build();
        }

        GroupState state = existing == null ? new GroupState() : existing;
        groups.put(groupId, state);
        state.members.add(creatorId);
        state.leaderId = creatorId;
        playerGroups.put(creatorId, groupId);
        transition.affectAll(state.members);
        transition.leadershipChanged(groupId, null, creatorId);
        return transition.build();
    }

    synchronized Transition join(UUID groupId, UUID playerId) {
        Objects.requireNonNull(groupId, "groupId");
        Objects.requireNonNull(playerId, "playerId");

        UUID currentGroupId = playerGroups.get(playerId);
        if (groupId.equals(currentGroupId)) {
            return Transition.none();
        }

        TransitionBuilder transition = new TransitionBuilder();
        detachFromPreviousGroup(groupId, playerId, transition);

        GroupState state = groups.computeIfAbsent(groupId, ignored -> new GroupState());
        if (state.members.add(playerId)) {
            playerGroups.put(playerId, groupId);
            transition.affect(playerId);
        }
        return transition.build();
    }

    synchronized Transition leave(UUID groupId, UUID playerId) {
        Objects.requireNonNull(groupId, "groupId");
        Objects.requireNonNull(playerId, "playerId");

        UUID trackedGroupId = playerGroups.get(playerId);
        if (trackedGroupId == null || !trackedGroupId.equals(groupId)) {
            return Transition.none();
        }

        TransitionBuilder transition = new TransitionBuilder();
        removeMember(trackedGroupId, playerId, transition);
        return transition.build();
    }

    synchronized Transition disconnect(UUID playerId) {
        Objects.requireNonNull(playerId, "playerId");
        UUID groupId = playerGroups.get(playerId);
        if (groupId == null) {
            return Transition.none();
        }

        TransitionBuilder transition = new TransitionBuilder();
        removeMember(groupId, playerId, transition);
        return transition.build();
    }

    synchronized Transition removeGroup(UUID groupId) {
        Objects.requireNonNull(groupId, "groupId");
        GroupState state = groups.remove(groupId);
        if (state == null) {
            return Transition.none();
        }

        state.members.forEach(playerId -> playerGroups.remove(playerId, groupId));
        TransitionBuilder transition = new TransitionBuilder();
        transition.affectAll(state.members);
        if (state.leaderId != null) {
            transition.leadershipChanged(groupId, state.leaderId, null);
        }
        return transition.build();
    }

    synchronized Authorization authorize(UUID groupId, UUID playerId) {
        GroupState state = groups.get(Objects.requireNonNull(groupId, "groupId"));
        if (state == null || state.leaderId == null) {
            return Authorization.UNKNOWN_LEADER;
        }
        return state.leaderId.equals(Objects.requireNonNull(playerId, "playerId"))
                ? Authorization.LEADER
                : Authorization.NOT_LEADER;
    }

    synchronized PlayerLeadershipState stateFor(UUID playerId) {
        UUID groupId = playerGroups.get(Objects.requireNonNull(playerId, "playerId"));
        if (groupId == null) {
            return PlayerLeadershipState.notInGroup();
        }
        GroupState state = groups.get(groupId);
        if (state == null || !state.members.contains(playerId)) {
            return PlayerLeadershipState.notInGroup();
        }
        return new PlayerLeadershipState(groupId, state.leaderId);
    }

    synchronized Transition clear() {
        TransitionBuilder transition = new TransitionBuilder();
        transition.affectAll(playerGroups.keySet());
        groups.clear();
        playerGroups.clear();
        return transition.build();
    }

    private void detachFromPreviousGroup(UUID targetGroupId, UUID playerId, TransitionBuilder transition) {
        UUID previousGroupId = playerGroups.get(playerId);
        if (previousGroupId != null && !previousGroupId.equals(targetGroupId)) {
            removeMember(previousGroupId, playerId, transition);
        }
    }

    private void removeMember(UUID groupId, UUID playerId, TransitionBuilder transition) {
        GroupState state = groups.get(groupId);
        boolean wasTracked = playerGroups.remove(playerId, groupId);
        if (state == null || !state.members.remove(playerId)) {
            if (wasTracked) {
                transition.affect(playerId);
            }
            return;
        }

        transition.affect(playerId);
        if (playerId.equals(state.leaderId)) {
            UUID nextLeaderId = state.members.stream().findFirst().orElse(null);
            UUID previousLeaderId = state.leaderId;
            state.leaderId = nextLeaderId;
            transition.affectAll(state.members);
            transition.leadershipChanged(groupId, previousLeaderId, nextLeaderId);
        }

        if (state.members.isEmpty()) {
            groups.remove(groupId, state);
        }
    }

    enum Authorization {
        LEADER,
        NOT_LEADER,
        UNKNOWN_LEADER
    }

    record PlayerLeadershipState(UUID groupId, UUID leaderId) {

        static PlayerLeadershipState notInGroup() {
            return new PlayerLeadershipState(null, null);
        }

        boolean inGroup() {
            return groupId != null;
        }

        boolean leaderKnown() {
            return leaderId != null;
        }
    }

    record LeadershipChange(UUID groupId, UUID previousLeaderId, UUID newLeaderId) {
    }

    record Transition(Set<UUID> affectedPlayerIds, List<LeadershipChange> leadershipChanges) {

        Transition {
            affectedPlayerIds = Set.copyOf(affectedPlayerIds);
            leadershipChanges = List.copyOf(leadershipChanges);
        }

        static Transition none() {
            return new Transition(Set.of(), List.of());
        }

        boolean changed() {
            return !affectedPlayerIds.isEmpty() || !leadershipChanges.isEmpty();
        }
    }

    private static final class GroupState {
        private final LinkedHashSet<UUID> members = new LinkedHashSet<>();
        private UUID leaderId;
    }

    private static final class TransitionBuilder {
        private final LinkedHashSet<UUID> affectedPlayerIds = new LinkedHashSet<>();
        private final List<LeadershipChange> leadershipChanges = new ArrayList<>();

        void affect(UUID playerId) {
            affectedPlayerIds.add(playerId);
        }

        void affectAll(Iterable<UUID> playerIds) {
            playerIds.forEach(affectedPlayerIds::add);
        }

        void leadershipChanged(UUID groupId, UUID previousLeaderId, UUID newLeaderId) {
            leadershipChanges.add(new LeadershipChange(groupId, previousLeaderId, newLeaderId));
        }

        Transition build() {
            if (affectedPlayerIds.isEmpty() && leadershipChanges.isEmpty()) {
                return Transition.none();
            }
            return new Transition(affectedPlayerIds, leadershipChanges);
        }
    }
}
