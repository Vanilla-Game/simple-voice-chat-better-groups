package ru.vanillagame.voicechat.bettergroups.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import ru.vanillagame.voicechat.bettergroups.client.GroupMuteClient;

/** A compact button with a pixel icon and a full text label for narration. */
public final class GroupPauseButton extends Button {

    public GroupPauseButton(int x, int y, OnPress onPress) {
        super(x, y, 20, 20, Component.empty(), onPress, DEFAULT_NARRATION);
    }

    @Override
    protected void renderContents(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        renderDefaultSprite(graphics);
        GroupPauseIcon.draw(graphics::fill, getX() + 6, getY() + 6,
                GroupMuteClient.isMuted(), GroupMuteClient.isPending(), active);
    }
}
