package ru.vanillagame.voicechat.groupmanagement;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RequestStoreTest {

    private static final Instant NOW = Instant.parse("2026-08-09T10:00:00Z");

    @Test
    void lookupIsNotBoundToASpecificApprover() {
        RequestStore store = new RequestStore(Clock.fixed(NOW, ZoneOffset.UTC), Duration.ofMinutes(5));
        UUID requesterId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();

        String token = store.create(requesterId, groupId);
        RequestStore.Lookup lookup = store.lookup(token);

        assertEquals(RequestStore.LookupStatus.VALID, lookup.status());
        assertEquals(requesterId, lookup.request().requesterId());
        assertEquals(groupId, lookup.request().groupId());
    }

    @Test
    void requestsExpireAndAreConsumedOnce() {
        Instant[] now = {NOW};
        Clock clock = new Clock() {
            @Override
            public java.time.ZoneId getZone() {
                return ZoneOffset.UTC;
            }

            @Override
            public Clock withZone(java.time.ZoneId zone) {
                return this;
            }

            @Override
            public Instant instant() {
                return now[0];
            }
        };
        RequestStore store = new RequestStore(clock, Duration.ofMinutes(5));
        UUID requesterId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();

        String token = store.create(requesterId, groupId);
        RequestStore.Request request = store.lookup(token).request();
        assertTrue(store.consume(token, request));
        assertFalse(store.consume(token, request));
        assertEquals(RequestStore.LookupStatus.NOT_FOUND, store.lookup(token).status());

        String expiringToken = store.create(requesterId, groupId);
        now[0] = NOW.plus(Duration.ofMinutes(6));
        assertEquals(RequestStore.LookupStatus.EXPIRED, store.lookup(expiringToken).status());
    }

    @Test
    void newRequestForSamePairSupersedesTheOldOne() {
        RequestStore store = new RequestStore(Clock.fixed(NOW, ZoneOffset.UTC), Duration.ofMinutes(5));
        UUID requesterId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();

        String first = store.create(requesterId, groupId);
        String second = store.create(requesterId, groupId);

        assertNotEquals(first, second);
        assertEquals(RequestStore.LookupStatus.NOT_FOUND, store.lookup(first).status());
        assertEquals(RequestStore.LookupStatus.VALID, store.lookup(second).status());
    }

    @Test
    void invalidationRemovesByGroupAndByRequester() {
        RequestStore store = new RequestStore(Clock.fixed(NOW, ZoneOffset.UTC), Duration.ofMinutes(5));
        UUID requesterId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();

        String byGroup = store.create(requesterId, groupId);
        store.invalidateGroup(groupId);
        assertEquals(RequestStore.LookupStatus.NOT_FOUND, store.lookup(byGroup).status());

        String byRequester = store.create(requesterId, UUID.randomUUID());
        store.invalidateRequester(requesterId);
        assertEquals(RequestStore.LookupStatus.NOT_FOUND, store.lookup(byRequester).status());
    }
}
