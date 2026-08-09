package ru.vanillagame.voicechat.groupmanagement;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

final class InviteCooldownStore {

    private final Map<PairKey, Instant> nextAllowedAt = new HashMap<>();
    private final Clock clock;
    private final Duration cooldown;

    InviteCooldownStore(Clock clock, Duration cooldown) {
        this.clock = Objects.requireNonNull(clock, "clock");
        this.cooldown = Objects.requireNonNull(cooldown, "cooldown");
        if (cooldown.isNegative()) {
            throw new IllegalArgumentException("cooldown must not be negative");
        }
    }

    synchronized Attempt tryAcquire(UUID inviterId, UUID targetId) {
        PairKey key = new PairKey(inviterId, targetId);
        if (cooldown.isZero()) {
            return Attempt.allowedAttempt();
        }

        Instant now = clock.instant();
        Instant nextAllowed = nextAllowedAt.get(key);
        if (nextAllowed != null && now.isBefore(nextAllowed)) {
            long remainingMillis = Duration.between(now, nextAllowed).toMillis();
            long remainingSeconds = Math.max(1L, (remainingMillis + 999L) / 1_000L);
            return Attempt.blocked(remainingSeconds);
        }

        nextAllowedAt.put(key, now.plus(cooldown));
        return Attempt.allowedAttempt();
    }

    synchronized void invalidate(UUID playerId) {
        Objects.requireNonNull(playerId, "playerId");
        nextAllowedAt.keySet().removeIf(
                key -> key.inviterId().equals(playerId) || key.targetId().equals(playerId)
        );
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

    private record PairKey(UUID inviterId, UUID targetId) {
        PairKey {
            Objects.requireNonNull(inviterId, "inviterId");
            Objects.requireNonNull(targetId, "targetId");
        }
    }
}
