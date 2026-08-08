package ru.vanillagame.voicechat.grouptools.client.mixin;

import de.maxhenkel.voicechat.gui.group.GroupEntry;
import de.maxhenkel.voicechat.gui.volume.AdjustVolumeSlider;
import de.maxhenkel.voicechat.gui.widgets.ListScreenEntryBase;
import de.maxhenkel.voicechat.voice.client.ClientManager;
import de.maxhenkel.voicechat.voice.common.PlayerState;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.vanillagame.voicechat.grouptools.client.ServerSupport;

@Mixin(value = GroupEntry.class, remap = false)
public abstract class GroupEntryMixin extends ListScreenEntryBase<GroupEntry> {

    @Unique
    private static final int VOICE_CHAT_GROUP_TOOLS$BUTTON_GAP = 2;

    @Unique
    private static final int VOICE_CHAT_GROUP_TOOLS$BUTTON_SIZE = 20;

    @Shadow
    protected PlayerState state;

    @Shadow
    @Final
    protected AdjustVolumeSlider volumeSlider;

    @Unique
    private Button voiceChatGroupTools$kickButton;

    @Inject(method = "<init>", at = @At("RETURN"), remap = false)
    private void voiceChatGroupTools$addKickButton(CallbackInfo callbackInfo) {
        voiceChatGroupTools$kickButton = Button.builder(Component.literal("×"), button -> {
                    ServerSupport.kick(state.getName());
                })
                .bounds(0, 0, VOICE_CHAT_GROUP_TOOLS$BUTTON_SIZE, VOICE_CHAT_GROUP_TOOLS$BUTTON_SIZE)
                .build();
        voiceChatGroupTools$kickButton.visible = false;
        children.add(voiceChatGroupTools$kickButton);
    }

    @Redirect(
            method = "extractContent",
            at = @At(
                    value = "INVOKE",
                    target = "Lde/maxhenkel/voicechat/gui/volume/AdjustVolumeSlider;setWidth(I)V"
            ),
            remap = false
    )
    private void voiceChatGroupTools$leaveSpaceForKickButton(AdjustVolumeSlider slider, int width) {
        if (!ServerSupport.isAvailable()) {
            slider.setWidth(width);
            return;
        }
        int reserved = VOICE_CHAT_GROUP_TOOLS$BUTTON_SIZE + VOICE_CHAT_GROUP_TOOLS$BUTTON_GAP;
        slider.setWidth(Math.max(0, width - reserved));
    }

    @Inject(method = "extractContent", at = @At("TAIL"), remap = false)
    private void voiceChatGroupTools$renderKickButton(
            GuiGraphicsExtractor guiGraphics,
            int mouseX,
            int mouseY,
            boolean hovered,
            float delta,
            CallbackInfo callbackInfo
    ) {
        boolean isSelf = ClientManager.getPlayerStateManager().getOwnID().equals(state.getUuid());
        boolean visible = hovered && !isSelf && ServerSupport.isAvailable();
        voiceChatGroupTools$kickButton.visible = visible;
        if (!visible) {
            return;
        }

        int x = volumeSlider.getX() - VOICE_CHAT_GROUP_TOOLS$BUTTON_SIZE - VOICE_CHAT_GROUP_TOOLS$BUTTON_GAP;
        int y = volumeSlider.getY();
        voiceChatGroupTools$kickButton.setPosition(x, y);
        voiceChatGroupTools$kickButton.setTooltip(Tooltip.create(Component.translatable(
                "gui.voicechat_group_tools.kick",
                state.getName()
        )));
        voiceChatGroupTools$kickButton.extractRenderState(guiGraphics, mouseX, mouseY, delta);
    }
}
