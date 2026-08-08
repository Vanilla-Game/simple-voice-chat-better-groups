package ru.vanillagame.voicechat.grouptools;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

final class InviteStore {

    static final Duration DEFAULT_TTL = Duration.ofMinutes(5);

    private final Map<String, Invite> invites;
    private final Clock clock;
    private final Duration ttl;
    private final TokenGenerator tokenGenerator;

    InviteStore(Clock clock) {
        this(clock, DEFAULT_TTL, new SecureTokenGenerator());
    }

    InviteStore(Clock clock, Duration ttl, TokenGenerator tokenGenerator) {
        this.invites = new HashMap<>();
        this.clock = Objects.requireNonNull(clock, "clock");
        this.ttl = Objects.requireNonNull(ttl, "ttl");
        this.tokenGenerator = Objects.requireNonNull(tokenGenerator, "tokenGenerator");
        if (ttl.isZero() || ttl.isNegative()) {
            throw new IllegalArgumentException("ttl must be positive");
        }
    }

    synchronized String create(UUID invitedPlayerId, UUID groupId) {
        Objects.requireNonNull(invitedPlayerId, "invitedPlayerId");
        Objects.requireNonNull(groupId, "groupId");
        cleanupExpired();

        invites.entrySet().removeIf(entry -> {
            Invite invite = entry.getValue();
            return invite.invitedPlayerId().equals(invitedPlayerId) && invite.groupId().equals(groupId);
        });

        String token;
        do {
            token = tokenGenerator.generate();
        } while (invites.containsKey(token));

        invites.put(token, new Invite(invitedPlayerId, groupId, clock.instant().plus(ttl)));
        return token;
    }

    synchronized Lookup lookup(String token, UUID acceptingPlayerId) {
        Objects.requireNonNull(token, "token");
        Objects.requireNonNull(acceptingPlayerId, "acceptingPlayerId");

        Invite invite = invites.get(token);
        if (invite == null) {
            return new Lookup(LookupStatus.NOT_FOUND, null);
        }
        if (!clock.instant().isBefore(invite.expiresAt())) {
            invites.remove(token);
            return new Lookup(LookupStatus.EXPIRED, null);
        }
        if (!invite.invitedPlayerId().equals(acceptingPlayerId)) {
            return new Lookup(LookupStatus.WRONG_PLAYER, null);
        }
        return new Lookup(LookupStatus.VALID, invite);
    }

    synchronized boolean consume(String token, Invite expectedInvite) {
        Objects.requireNonNull(token, "token");
        Objects.requireNonNull(expectedInvite, "expectedInvite");
        return invites.remove(token, expectedInvite);
    }

    synchronized void invalidateGroup(UUID groupId) {
        invites.values().removeIf(invite -> invite.groupId().equals(groupId));
    }

    synchronized void invalidatePlayer(UUID playerId) {
        invites.values().removeIf(invite -> invite.invitedPlayerId().equals(playerId));
    }

    synchronized void cleanupExpired() {
        Instant now = clock.instant();
        invites.values().removeIf(invite -> !now.isBefore(invite.expiresAt()));
    }

    synchronized void clear() {
        invites.clear();
    }

    record Invite(UUID invitedPlayerId, UUID groupId, Instant expiresAt) {
        Invite {
            Objects.requireNonNull(invitedPlayerId, "invitedPlayerId");
            Objects.requireNonNull(groupId, "groupId");
            Objects.requireNonNull(expiresAt, "expiresAt");
        }
    }

    enum LookupStatus {
        VALID,
        NOT_FOUND,
        EXPIRED,
        WRONG_PLAYER
    }

    record Lookup(LookupStatus status, Invite invite) {
    }

    @FunctionalInterface
    interface TokenGenerator {
        String generate();
    }

    static final class SecureTokenGenerator implements TokenGenerator {

        private static final int TOKEN_BYTES = 24;
        private final SecureRandom random = new SecureRandom();

        @Override
        public String generate() {
            byte[] bytes = new byte[TOKEN_BYTES];
            random.nextBytes(bytes);
            return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        }
    }
}
