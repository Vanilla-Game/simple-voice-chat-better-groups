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
    void blocksRepeatedInvitesUntilCooldownExpires() {
        MutableClock clock = new MutableClock(START);
        InviteCooldownStore cooldowns = new InviteCooldownStore(clock, Duration.ofSeconds(10));
        UUID playerId = UUID.randomUUID();

        assertTrue(cooldowns.tryAcquire(playerId).allowed());

        clock.advance(Duration.ofMillis(1_001));
        InviteCooldownStore.Attempt blocked = cooldowns.tryAcquire(playerId);
        assertFalse(blocked.allowed());
        assertEquals(9L, blocked.retryAfterSeconds());

        clock.advance(Duration.ofMillis(8_999));
        assertTrue(cooldowns.tryAcquire(playerId).allowed());
    }

    @Test
    void cooldownIsTrackedSeparatelyForEachPlayer() {
        InviteCooldownStore cooldowns = new InviteCooldownStore(
                Clock.fixed(START, ZoneOffset.UTC),
                Duration.ofSeconds(10)
        );

        assertTrue(cooldowns.tryAcquire(UUID.randomUUID()).allowed());
        assertTrue(cooldowns.tryAcquire(UUID.randomUUID()).allowed());
    }

    @Test
    void zeroCooldownDisablesThrottling() {
        InviteCooldownStore cooldowns = new InviteCooldownStore(
                Clock.fixed(START, ZoneOffset.UTC),
                Duration.ZERO
        );
        UUID playerId = UUID.randomUUID();

        assertTrue(cooldowns.tryAcquire(playerId).allowed());
        assertTrue(cooldowns.tryAcquire(playerId).allowed());
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
