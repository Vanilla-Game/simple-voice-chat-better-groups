package ru.vanillagame.voicechat.groupmanagement.client.gui;

import de.maxhenkel.voicechat.gui.GameProfileUtils;
import de.maxhenkel.voicechat.gui.widgets.ListScreenEntryBase;
import de.maxhenkel.voicechat.voice.common.PlayerState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.player.PlayerSkin;

public class InvitePlayerEntry extends ListScreenEntryBase<InvitePlayerEntry> {

    protected static final int SKIN_SIZE = 24;
    protected static final int PADDING = 4;
    protected static final int BG_FILL = ARGB.color(255, 74, 74, 74);
    protected static final int BG_FILL_HOVERED = ARGB.color(255, 90, 90, 90);

    protected final Minecraft minecraft;
    protected final PlayerState state;

    public InvitePlayerEntry(PlayerState state) {
        this.minecraft = Minecraft.getInstance();
        this.state = state;
    }

    public PlayerState getState() {
        return state;
    }

    @Override
    public void extractContent(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, boolean hovered, float delta) {
        int left = getContentX();
        int top = getContentY();
        int width = getContentWidth();
        int height = getContentHeight();
        int skinX = left + PADDING;
        int skinY = top + (height - SKIN_SIZE) / 2;

        guiGraphics.fill(left, top, left + width, top + height, hovered ? BG_FILL_HOVERED : BG_FILL);

        PlayerSkin skin = GameProfileUtils.getSkin(state.getUuid());
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, skin.body().texturePath(), skinX, skinY, 8, 8, SKIN_SIZE, SKIN_SIZE, 8, 8, 64, 64);
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, skin.body().texturePath(), skinX, skinY, 40, 8, SKIN_SIZE, SKIN_SIZE, 8, 8, 64, 64);

        Component name = Component.literal(state.getName());
        int textX = skinX + SKIN_SIZE + PADDING;
        int textY = top + (height - minecraft.font.lineHeight) / 2;
        int textSpace = width - PADDING - SKIN_SIZE - PADDING - PADDING;
        if (minecraft.font.width(name) > textSpace) {
            guiGraphics.textRenderer().acceptScrollingWithDefaultCenter(
                    name, textX, textX + textSpace, textY, textY + minecraft.font.lineHeight);
        } else {
            guiGraphics.text(minecraft.font, name, textX, textY, 0xFFFFFFFF, false);
        }
    }
}
