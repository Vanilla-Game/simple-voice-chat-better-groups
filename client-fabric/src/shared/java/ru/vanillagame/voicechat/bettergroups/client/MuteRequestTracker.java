package ru.vanillagame.voicechat.bettergroups.client;

// Client-thread request state; the microphone thread only reads blocked.
public final class MuteRequestTracker {
    private int sequence;
    private int pending;
    private boolean targetMuted;
    private volatile boolean blocked;

    public int begin(boolean currentlyMuted) {
        if (pending == 0) targetMuted = !currentlyMuted;
        pending = nextId();
        blocked = true;
        return pending;
    }

    public int nextId() {
        sequence = sequence == Integer.MAX_VALUE ? 1 : sequence + 1;
        return sequence;
    }

    public boolean acknowledge(int id) {
        if (id <= 0 || id != pending) return false;
        pending = 0;
        blocked = false;
        return true;
    }

    public boolean isPending() { return pending != 0; }
    public boolean targetMuted() { return targetMuted; }
    public boolean isBlocked() { return blocked; }
    public void pause() { blocked = true; }

    public void clear() {
        pending = 0;
        blocked = false;
    }
}
