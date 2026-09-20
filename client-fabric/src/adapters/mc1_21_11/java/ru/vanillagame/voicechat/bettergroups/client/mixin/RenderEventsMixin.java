package ru.vanillagame.voicechat.bettergroups.client.mixin;

import de.maxhenkel.voicechat.VoicechatClient;
import de.maxhenkel.voicechat.voice.client.ClientManager;
import de.maxhenkel.voicechat.voice.client.RenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.vanillagame.voicechat.bettergroups.client.GroupMuteClient;

@Mixin(value = RenderEvents.class, remap = false)
public abstract class RenderEventsMixin {

    @Shadow @Final private static Identifier GROUP_ICON;

    @Shadow
    private void renderIcon(GuiGraphics graphics, Identifier icon) {
        throw new AssertionError();
    }

    // TAIL runs after SVC's visibility checks, independently of showGroupHud.
    @Inject(method = "onRenderHUD", at = @At("TAIL"), remap = false)
    private void svcBetterGroups$renderGroupMute(GuiGraphics graphics, float delta, CallbackInfo ci) {
        var manager = ClientManager.getPlayerStateManager();
        if (!GroupMuteClient.isMuted() || manager.isDisconnected()) {
            return;
        }
        var config = VoicechatClient.CLIENT_CONFIG;
        var window = Minecraft.getInstance().getWindow();
        int x = config.hudIconPosX.get();
        int y = config.hudIconPosY.get();
        float scale = config.hudIconScale.get().floatValue();
        var pose = graphics.pose();
        pose.pushMatrix();
        // Keep a separate slot beside the microphone/speaker icon, toward the screen interior.
        pose.translate((x < 0 ? -20F : 20F) * scale, 0F);
        renderIcon(graphics, GROUP_ICON);
        pose.translate(x + (x < 0 ? window.getGuiScaledWidth() : 0),
                y + (y < 0 ? window.getGuiScaledHeight() : 0));
        pose.scale(scale, scale);
        int left = x < 0 ? -16 : 0;
        int top = y < 0 ? -16 : 0;
        // Pixel-aligned slash, with a dark border for contrast on bright backgrounds.
        for (int pixel = 2; pixel < 14; pixel++) {
            graphics.fill(left + pixel - 1, top + 14 - pixel,
                    left + pixel + 2, top + 17 - pixel, 0xFF202020);
        }
        for (int pixel = 2; pixel < 14; pixel++) {
            graphics.fill(left + pixel, top + 14 - pixel,
                    left + pixel + 2, top + 16 - pixel, 0xFFFF3030);
        }
        pose.popMatrix();
    }
}
