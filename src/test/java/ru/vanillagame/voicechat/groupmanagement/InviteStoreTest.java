package ru.vanillagame.voicechat.groupmanagement;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InviteStoreTest {

    private static final Instant START = Instant.parse("2026-08-08T10:00:00Z");

    @Test
    void secureTokensAreRandomAndUrlSafe() {
        InviteStore.SecureTokenGenerator generator = new InviteStore.SecureTokenGenerator();

        String first = generator.generate();
        String second = generator.generate();

        assertNotEquals(first, second);
        assertTrue(first.matches("[A-Za-z0-9_-]{32}"));
        assertTrue(second.matches("[A-Za-z0-9_-]{32}"));
    }

    @Test
    void inviteExpiresAfterFiveMinutes() {
        MutableClock clock = new MutableClock(START);
        InviteStore store = new InviteStore(clock, Duration.ofMinutes(5), () -> "token");
        UUID playerId = UUID.randomUUID();

        store.create(playerId, UUID.randomUUID(), UUID.randomUUID(), "Inviter");
        clock.advance(Duration.ofMinutes(5).minusMillis(1));
        assertEquals(InviteStore.LookupStatus.VALID, store.lookup("token", playerId).status());

        clock.advance(Duration.ofMillis(1));
        assertEquals(InviteStore.LookupStatus.EXPIRED, store.lookup("token", playerId).status());
        assertEquals(InviteStore.LookupStatus.NOT_FOUND, store.lookup("token", playerId).status());
    }

    @Test
    void inviteCanOnlyBeConsumedOnce() {
        InviteStore store = new InviteStore(
                Clock.fixed(START, ZoneOffset.UTC),
                Duration.ofMinutes(5),
                () -> "one-time-token"
        );
        UUID playerId = UUID.randomUUID();
        store.create(playerId, UUID.randomUUID(), UUID.randomUUID(), "Inviter");
        InviteStore.Invite invite = store.lookup("one-time-token", playerId).invite();

        assertTrue(store.consume("one-time-token", invite));
        assertFalse(store.consume("one-time-token", invite));
        assertEquals(InviteStore.LookupStatus.NOT_FOUND, store.lookup("one-time-token", playerId).status());
    }

    @Test
    void inviteIsBoundToPlayerAndGroupUuids() {
        InviteStore store = new InviteStore(
                Clock.fixed(START, ZoneOffset.UTC),
                Duration.ofMinutes(5),
                () -> "bound-token"
        );
        UUID invitedPlayerId = UUID.randomUUID();
        UUID otherPlayerId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();
        store.create(invitedPlayerId, groupId, UUID.randomUUID(), "Inviter");

        assertEquals(
                InviteStore.LookupStatus.WRONG_PLAYER,
                store.lookup("bound-token", otherPlayerId).status()
        );
        InviteStore.Lookup validLookup = store.lookup("bound-token", invitedPlayerId);
        assertEquals(InviteStore.LookupStatus.VALID, validLookup.status());
        assertEquals(invitedPlayerId, validLookup.invite().invitedPlayerId());
        assertEquals(groupId, validLookup.invite().groupId());
    }

    @Test
    void newerInviteForSamePlayerAndGroupInvalidatesOlderToken() {
        String[] tokens = {"old-token", "new-token"};
        int[] index = {0};
        InviteStore store = new InviteStore(
                Clock.fixed(START, ZoneOffset.UTC),
                Duration.ofMinutes(5),
                () -> tokens[index[0]++]
        );
        UUID playerId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();

        store.create(playerId, groupId, UUID.randomUUID(), "Inviter");
        store.create(playerId, groupId, UUID.randomUUID(), "Inviter");

        assertEquals(InviteStore.LookupStatus.NOT_FOUND, store.lookup("old-token", playerId).status());
        assertEquals(InviteStore.LookupStatus.VALID, store.lookup("new-token", playerId).status());
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
