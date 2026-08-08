package ru.vanillagame.voicechat.grouptools;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

final class InviteCooldownStore {

    private final Map<UUID, Instant> nextAllowedAt = new HashMap<>();
    private final Clock clock;
    private final Duration cooldown;

    InviteCooldownStore(Clock clock, Duration cooldown) {
        this.clock = Objects.requireNonNull(clock, "clock");
        this.cooldown = Objects.requireNonNull(cooldown, "cooldown");
        if (cooldown.isNegative()) {
            throw new IllegalArgumentException("cooldown must not be negative");
        }
    }

    synchronized Attempt tryAcquire(UUID playerId) {
        Objects.requireNonNull(playerId, "playerId");
        if (cooldown.isZero()) {
            return Attempt.allowedAttempt();
        }

        Instant now = clock.instant();
        Instant nextAllowed = nextAllowedAt.get(playerId);
        if (nextAllowed != null && now.isBefore(nextAllowed)) {
            long remainingMillis = Duration.between(now, nextAllowed).toMillis();
            long remainingSeconds = Math.max(1L, (remainingMillis + 999L) / 1_000L);
            return Attempt.blocked(remainingSeconds);
        }

        nextAllowedAt.put(playerId, now.plus(cooldown));
        return Attempt.allowedAttempt();
    }

    synchronized void invalidate(UUID playerId) {
        nextAllowedAt.remove(Objects.requireNonNull(playerId, "playerId"));
    }

    synchronized void cleanupExpired() {
        Instant now = clock.instant();
        nextAllowedAt.values().removeIf(nextAllowed -> !now.isBefore(nextAllowed));
    }

    synchronized void clear() {
        nextAllowedAt.clear();
    }

    record Attempt(boolean allowed, long retryAfterSeconds) {

        private static Attempt allowedAttempt() {
            return new Attempt(true, 0L);
        }

        private static Attempt blocked(long retryAfterSeconds) {
            return new Attempt(false, retryAfterSeconds);
        }
    }
}
