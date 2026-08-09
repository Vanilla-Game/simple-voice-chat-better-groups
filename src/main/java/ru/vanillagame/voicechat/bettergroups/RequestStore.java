package ru.vanillagame.voicechat.bettergroups;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

// Join requests mirror invites but flow the other way: the token is handed to
// the group leader, and the approver is whoever currently holds leadership at
// click time — so unlike InviteStore, a lookup is not bound to a fixed player.
final class RequestStore {

    private final Map<String, Request> requests;
    private final Clock clock;
    private final Duration ttl;
    private final TokenGenerator tokenGenerator;

    RequestStore(Clock clock, Duration ttl) {
        this(clock, ttl, new SecureTokenGenerator());
    }

    RequestStore(Clock clock, Duration ttl, TokenGenerator tokenGenerator) {
        this.requests = new HashMap<>();
        this.clock = Objects.requireNonNull(clock, "clock");
        this.ttl = Objects.requireNonNull(ttl, "ttl");
        this.tokenGenerator = Objects.requireNonNull(tokenGenerator, "tokenGenerator");
        if (ttl.isZero() || ttl.isNegative()) {
            throw new IllegalArgumentException("ttl must be positive");
        }
    }

    synchronized String create(UUID requesterId, UUID groupId) {
        Objects.requireNonNull(requesterId, "requesterId");
        Objects.requireNonNull(groupId, "groupId");
        cleanupExpired();

        requests.entrySet().removeIf(entry -> {
            Request request = entry.getValue();
            return request.requesterId().equals(requesterId) && request.groupId().equals(groupId);
        });

        String token;
        do {
            token = tokenGenerator.generate();
        } while (requests.containsKey(token));

        requests.put(token, new Request(requesterId, groupId, clock.instant().plus(ttl)));
        return token;
    }

    synchronized Lookup lookup(String token) {
        Objects.requireNonNull(token, "token");

        Request request = requests.get(token);
        if (request == null) {
            return new Lookup(LookupStatus.NOT_FOUND, null);
        }
        if (!clock.instant().isBefore(request.expiresAt())) {
            requests.remove(token);
            return new Lookup(LookupStatus.EXPIRED, null);
        }
        return new Lookup(LookupStatus.VALID, request);
    }

    synchronized boolean consume(String token, Request expectedRequest) {
        Objects.requireNonNull(token, "token");
        Objects.requireNonNull(expectedRequest, "expectedRequest");
        return requests.remove(token, expectedRequest);
    }

    synchronized void invalidateGroup(UUID groupId) {
        requests.values().removeIf(request -> request.groupId().equals(groupId));
    }

    synchronized void invalidateRequester(UUID requesterId) {
        requests.values().removeIf(request -> request.requesterId().equals(requesterId));
    }

    synchronized void cleanupExpired() {
        Instant now = clock.instant();
        requests.values().removeIf(request -> !now.isBefore(request.expiresAt()));
    }

    synchronized void clear() {
        requests.clear();
    }

    record Request(UUID requesterId, UUID groupId, Instant expiresAt) {
        Request {
            Objects.requireNonNull(requesterId, "requesterId");
            Objects.requireNonNull(groupId, "groupId");
            Objects.requireNonNull(expiresAt, "expiresAt");
        }
    }

    enum LookupStatus {
        VALID,
        NOT_FOUND,
        EXPIRED
    }

    record Lookup(LookupStatus status, Request request) {
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
