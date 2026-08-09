package ru.vanillagame.voicechat.grouptools;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GroupLeadershipRegistryTest {

    @Test
    void creatorIsInitialLeaderAndOtherMembersAreNotAuthorized() {
        GroupLeadershipRegistry registry = new GroupLeadershipRegistry();
        UUID groupId = UUID.randomUUID();
        UUID creatorId = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();

        registry.createGroup(groupId, creatorId);
        registry.join(groupId, memberId);

        assertEquals(
                GroupLeadershipRegistry.Authorization.LEADER,
                registry.authorize(groupId, creatorId)
        );
        assertEquals(
                GroupLeadershipRegistry.Authorization.NOT_LEADER,
                registry.authorize(groupId, memberId)
        );
    }

    @Test
    void leaderLeavePromotesLongestStandingMember() {
        GroupLeadershipRegistry registry = new GroupLeadershipRegistry();
        UUID groupId = UUID.randomUUID();
        UUID leaderId = UUID.randomUUID();
        UUID firstMemberId = UUID.randomUUID();
        UUID secondMemberId = UUID.randomUUID();
        registry.createGroup(groupId, leaderId);
        registry.join(groupId, firstMemberId);
        registry.join(groupId, secondMemberId);

        GroupLeadershipRegistry.Transition transition = registry.leave(groupId, leaderId);

        assertEquals(firstMemberId, registry.stateFor(firstMemberId).leaderId());
        assertEquals(firstMemberId, registry.stateFor(secondMemberId).leaderId());
        assertTrue(transition.affectedPlayerIds().contains(firstMemberId));
        assertEquals(1, transition.leadershipChanges().size());
    }

    @Test
    void ordinaryMemberLeaveDoesNotChangeLeader() {
        GroupLeadershipRegistry registry = new GroupLeadershipRegistry();
        UUID groupId = UUID.randomUUID();
        UUID leaderId = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();
        registry.createGroup(groupId, leaderId);
        registry.join(groupId, memberId);

        GroupLeadershipRegistry.Transition transition = registry.leave(groupId, memberId);

        assertEquals(leaderId, registry.stateFor(leaderId).leaderId());
        assertTrue(transition.leadershipChanges().isEmpty());
        assertNull(registry.stateFor(memberId).groupId());
    }

    @Test
    void movingBetweenGroupsUpdatesBothMembershipsWithoutLeaveEvent() {
        GroupLeadershipRegistry registry = new GroupLeadershipRegistry();
        UUID oldGroupId = UUID.randomUUID();
        UUID newGroupId = UUID.randomUUID();
        UUID movingLeaderId = UUID.randomUUID();
        UUID successorId = UUID.randomUUID();
        UUID newGroupLeaderId = UUID.randomUUID();
        registry.createGroup(oldGroupId, movingLeaderId);
        registry.join(oldGroupId, successorId);
        registry.createGroup(newGroupId, newGroupLeaderId);

        registry.join(newGroupId, movingLeaderId);

        assertEquals(successorId, registry.stateFor(successorId).leaderId());
        assertEquals(newGroupId, registry.stateFor(movingLeaderId).groupId());
        assertEquals(newGroupLeaderId, registry.stateFor(movingLeaderId).leaderId());
    }

    @Test
    void duplicateLeaveAndDisconnectAreIdempotent() {
        GroupLeadershipRegistry registry = new GroupLeadershipRegistry();
        UUID groupId = UUID.randomUUID();
        UUID leaderId = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();
        registry.createGroup(groupId, leaderId);
        registry.join(groupId, memberId);

        GroupLeadershipRegistry.Transition first = registry.leave(groupId, leaderId);
        GroupLeadershipRegistry.Transition duplicateLeave = registry.leave(groupId, leaderId);
        GroupLeadershipRegistry.Transition duplicateDisconnect = registry.disconnect(leaderId);

        assertTrue(first.changed());
        assertFalse(duplicateLeave.changed());
        assertFalse(duplicateDisconnect.changed());
        assertEquals(memberId, registry.stateFor(memberId).leaderId());
    }

    @Test
    void returningFormerLeaderJoinsAtEndAndDoesNotReclaimLeadership() {
        GroupLeadershipRegistry registry = new GroupLeadershipRegistry();
        UUID groupId = UUID.randomUUID();
        UUID formerLeaderId = UUID.randomUUID();
        UUID firstMemberId = UUID.randomUUID();
        UUID secondMemberId = UUID.randomUUID();
        registry.createGroup(groupId, formerLeaderId);
        registry.join(groupId, firstMemberId);
        registry.join(groupId, secondMemberId);
        registry.leave(groupId, formerLeaderId);
        registry.join(groupId, formerLeaderId);

        registry.leave(groupId, firstMemberId);

        assertEquals(secondMemberId, registry.stateFor(formerLeaderId).leaderId());
    }

    @Test
    void groupFirstObservedThroughJoinRemainsFailClosed() {
        GroupLeadershipRegistry registry = new GroupLeadershipRegistry();
        UUID groupId = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();

        registry.join(groupId, memberId);

        assertEquals(
                GroupLeadershipRegistry.Authorization.UNKNOWN_LEADER,
                registry.authorize(groupId, memberId)
        );
        assertFalse(registry.stateFor(memberId).leaderKnown());
    }

    @Test
    void removingGroupClearsAllTrackedMembers() {
        GroupLeadershipRegistry registry = new GroupLeadershipRegistry();
        UUID groupId = UUID.randomUUID();
        UUID leaderId = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();
        registry.createGroup(groupId, leaderId);
        registry.join(groupId, memberId);

        GroupLeadershipRegistry.Transition transition = registry.removeGroup(groupId);

        assertNull(registry.stateFor(leaderId).groupId());
        assertNull(registry.stateFor(memberId).groupId());
        assertEquals(2, transition.affectedPlayerIds().size());
        assertEquals(
                GroupLeadershipRegistry.Authorization.UNKNOWN_LEADER,
                registry.authorize(groupId, leaderId)
        );
    }
}
