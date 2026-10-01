package ru.vanillagame.voicechat.bettergroups.client.gui;

/** Pixel geometry shared by both Minecraft rendering APIs. */
public final class GroupPauseIcon {

    private GroupPauseIcon() {
    }

    public static void draw(PixelFill fill, int x, int y, boolean paused, boolean pending, boolean active) {
        int color = !active ? 0xFFA0A0A0 : paused ? 0xFFFF5555 : 0xFFFFFFFF;
        // Draw the same one-pixel shadow as Minecraft's button labels.
        drawShape(fill, x + 1, y + 1, paused, pending, 0xFF3F3F3F);
        drawShape(fill, x, y, paused, pending, color);
    }

    private static void drawShape(PixelFill fill, int x, int y, boolean paused, boolean pending, int color) {
        if (pending) {
            for (int offset = 0; offset <= 6; offset += 3) {
                fill.fill(x + offset, y + 3, x + offset + 2, y + 5, color);
            }
        } else if (paused) {
            // Right-facing play arrow: return to the group.
            for (int column = 0; column < 4; column++) {
                fill.fill(x + 2 + column, y + column, x + 3 + column, y + 8 - column, color);
            }
        } else {
            fill.fill(x, y, x + 3, y + 8, color);
            fill.fill(x + 5, y, x + 8, y + 8, color);
        }
    }

    @FunctionalInterface
    public interface PixelFill {
        void fill(int left, int top, int right, int bottom, int color);
    }
}
