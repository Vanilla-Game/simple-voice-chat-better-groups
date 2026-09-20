package ru.vanillagame.voicechat.bettergroups.client;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MuteRequestTrackerTest {
    @Test void keepsMicrophonePausedUntilMatchingAcknowledgement() {
        var tracker = new MuteRequestTracker();
        int request = tracker.begin(false);
        assertTrue(tracker.targetMuted());
        assertTrue(tracker.isBlocked());
        assertFalse(tracker.acknowledge(-1));
        assertFalse(tracker.acknowledge(0));
        assertFalse(tracker.acknowledge(request + 1));
        assertTrue(tracker.isBlocked());
        assertTrue(tracker.acknowledge(request));
        assertFalse(tracker.isBlocked());
    }

    @Test void secondPressRetriesPendingEnableWithoutAcceptingStaleReply() {
        var tracker = new MuteRequestTracker();
        int enable = tracker.begin(false);
        int retry = tracker.begin(true);
        assertTrue(tracker.targetMuted());
        assertFalse(tracker.acknowledge(enable));
        assertTrue(tracker.isBlocked());
        assertTrue(tracker.acknowledge(retry));
        assertFalse(tracker.isBlocked());
    }

    @Test void lostServerPolicyStaysPausedUntilExplicitResetOrNewAcknowledgement() {
        var tracker = new MuteRequestTracker();
        int old = tracker.begin(false);
        tracker.acknowledge(old);
        tracker.pause();
        assertFalse(tracker.acknowledge(old));
        assertTrue(tracker.isBlocked());
        int disable = tracker.begin(true);
        assertFalse(tracker.targetMuted());
        tracker.acknowledge(disable);
        assertFalse(tracker.isBlocked());
        tracker.pause();
        tracker.clear();
        assertFalse(tracker.isBlocked());
    }

    @Test void leavingDoesNotAllowOldAcknowledgementsToAffectANewGroup() {
        var tracker = new MuteRequestTracker();
        int old = tracker.begin(false);
        tracker.clear();
        int current = tracker.begin(false);
        assertNotEquals(old, current);
        assertFalse(tracker.acknowledge(old));
        assertTrue(tracker.isBlocked());
    }
}
