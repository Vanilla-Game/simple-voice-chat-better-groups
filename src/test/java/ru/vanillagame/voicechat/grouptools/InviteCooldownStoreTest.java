package ru.vanillagame.voicechat.grouptools;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InviteCooldownStoreTest {

    private static final Instant START = Instant.parse("2026-08-08T10:00:00Z");

    @Test
    void blocksRepeatedInvitesToSameTargetUntilCooldownExpires() {
        MutableClock clock = new MutableClock(START);
        InviteCooldownStore cooldowns = new InviteCooldownStore(clock, Duration.ofSeconds(10));
        UUID inviterId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();

        assertTrue(cooldowns.tryAcquire(inviterId, targetId).allowed());

        clock.advance(Duration.ofMillis(1_001));
        InviteCooldownStore.Attempt blocked = cooldowns.tryAcquire(inviterId, targetId);
        assertFalse(blocked.allowed());
        assertEquals(9L, blocked.retryAfterSeconds());

        clock.advance(Duration.ofMillis(8_999));
        assertTrue(cooldowns.tryAcquire(inviterId, targetId).allowed());
    }

    @Test
    void cooldownIsTrackedSeparatelyForEachInviterTargetPair() {
        InviteCooldownStore cooldowns = new InviteCooldownStore(
                Clock.fixed(START, ZoneOffset.UTC),
                Duration.ofSeconds(10)
        );
        UUID inviterId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();

        assertTrue(cooldowns.tryAcquire(inviterId, targetId).allowed());
        assertTrue(cooldowns.tryAcquire(inviterId, UUID.randomUUID()).allowed());
        assertTrue(cooldowns.tryAcquire(UUID.randomUUID(), targetId).allowed());
        assertFalse(cooldowns.tryAcquire(inviterId, targetId).allowed());
    }

    @Test
    void pairKeyIsDirectional() {
        InviteCooldownStore cooldowns = new InviteCooldownStore(
                Clock.fixed(START, ZoneOffset.UTC),
                Duration.ofSeconds(10)
        );
        UUID inviterId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();

        assertTrue(cooldowns.tryAcquire(inviterId, targetId).allowed());
        assertTrue(cooldowns.tryAcquire(targetId, inviterId).allowed());
    }

    @Test
    void invalidateRemovesPairsOnEitherSide() {
        InviteCooldownStore cooldowns = new InviteCooldownStore(
                Clock.fixed(START, ZoneOffset.UTC),
                Duration.ofSeconds(10)
        );
        UUID quittingPlayerId = UUID.randomUUID();
        UUID otherId = UUID.randomUUID();

        assertTrue(cooldowns.tryAcquire(quittingPlayerId, otherId).allowed());
        assertTrue(cooldowns.tryAcquire(otherId, quittingPlayerId).allowed());

        cooldowns.invalidate(quittingPlayerId);

        assertTrue(cooldowns.tryAcquire(quittingPlayerId, otherId).allowed());
        assertTrue(cooldowns.tryAcquire(otherId, quittingPlayerId).allowed());
    }

    @Test
    void zeroCooldownDisablesThrottling() {
        InviteCooldownStore cooldowns = new InviteCooldownStore(
                Clock.fixed(START, ZoneOffset.UTC),
                Duration.ZERO
        );
        UUID inviterId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();

        assertTrue(cooldowns.tryAcquire(inviterId, targetId).allowed());
        assertTrue(cooldowns.tryAcquire(inviterId, targetId).allowed());
    }

    private static final class MutableClock extends Clock {

        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        void advance(Duration duration) {
            instant = instant.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
