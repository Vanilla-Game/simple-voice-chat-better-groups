package ru.vanillagame.voicechat.groupmanagement.client.mixin;

import de.maxhenkel.voicechat.gui.group.GroupEntry;
import de.maxhenkel.voicechat.gui.volume.AdjustVolumeSlider;
import de.maxhenkel.voicechat.gui.widgets.ListScreenEntryBase;
import de.maxhenkel.voicechat.voice.client.ClientManager;
import de.maxhenkel.voicechat.voice.common.PlayerState;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.vanillagame.voicechat.groupmanagement.client.LeaderClientState;
import ru.vanillagame.voicechat.groupmanagement.client.ServerSupport;

@Mixin(value = GroupEntry.class, remap = false)
public abstract class GroupEntryMixin extends ListScreenEntryBase<GroupEntry> {

    @Unique
    private static final int SVC_GROUP_MANAGEMENT$BUTTON_GAP = 2;

    @Unique
    private static final int SVC_GROUP_MANAGEMENT$BUTTON_SIZE = 20;

    @Shadow
    protected PlayerState state;

    @Shadow
    @Final
    protected AdjustVolumeSlider volumeSlider;

    // The default font renders non-ASCII glyphs from the squished unifont
    // fallback, so the crown ships as a custom 8x8 bitmap glyph instead.
    @Unique
    private static final FontDescription SVC_GROUP_MANAGEMENT$CROWN_FONT = new FontDescription.Resource(
            Identifier.fromNamespaceAndPath("svc_group_management_client", "crown"));

    @Unique
    private Button svcGroupManagement$kickButton;

    @Redirect(
            method = "extractContent",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/network/chat/Component;literal(Ljava/lang/String;)Lnet/minecraft/network/chat/MutableComponent;"
            ),
            remap = true
    )
    private MutableComponent svcGroupManagement$markLeader(String playerName) {
        if (!LeaderClientState.isLeader(state.getUuid())) {
            return Component.literal(playerName);
        }
        return Component.empty()
                .append(Component.literal("\uE000").withStyle(style -> style
                        .withFont(SVC_GROUP_MANAGEMENT$CROWN_FONT)
                        .withColor(ChatFormatting.GOLD)))
                .append(Component.literal(" " + playerName).withStyle(ChatFormatting.WHITE));
    }

    @Inject(method = "<init>", at = @At("RETURN"), remap = false)
    private void svcGroupManagement$addKickButton(CallbackInfo callbackInfo) {
        svcGroupManagement$kickButton = Button.builder(Component.literal("×"), button -> {
                    ServerSupport.kick(state.getName());
                })
                .bounds(0, 0, SVC_GROUP_MANAGEMENT$BUTTON_SIZE, SVC_GROUP_MANAGEMENT$BUTTON_SIZE)
                .build();
        svcGroupManagement$kickButton.visible = false;
        children.add(svcGroupManagement$kickButton);
    }

    @Redirect(
            method = "extractContent",
            at = @At(
                    value = "INVOKE",
                    target = "Lde/maxhenkel/voicechat/gui/volume/AdjustVolumeSlider;setWidth(I)V"
            ),
            remap = false
    )
    private void svcGroupManagement$leaveSpaceForKickButton(AdjustVolumeSlider slider, int width) {
        if (!ServerSupport.isAvailable() || !LeaderClientState.isLocalPlayerLeader()) {
            slider.setWidth(width);
            return;
        }
        int reserved = SVC_GROUP_MANAGEMENT$BUTTON_SIZE + SVC_GROUP_MANAGEMENT$BUTTON_GAP;
        slider.setWidth(Math.max(0, width - reserved));
    }

    @Inject(method = "extractContent", at = @At("TAIL"), remap = false)
    private void svcGroupManagement$renderKickButton(
            GuiGraphicsExtractor guiGraphics,
            int mouseX,
            int mouseY,
            boolean hovered,
            float delta,
            CallbackInfo callbackInfo
    ) {
        boolean isSelf = ClientManager.getPlayerStateManager().getOwnID().equals(state.getUuid());
        boolean visible = hovered
                && !isSelf
                && ServerSupport.isAvailable()
                && LeaderClientState.isLocalPlayerLeader();
        svcGroupManagement$kickButton.visible = visible;
        if (!visible) {
            return;
        }

        int x = volumeSlider.getX() - SVC_GROUP_MANAGEMENT$BUTTON_SIZE - SVC_GROUP_MANAGEMENT$BUTTON_GAP;
        int y = volumeSlider.getY();
        svcGroupManagement$kickButton.setPosition(x, y);
        svcGroupManagement$kickButton.setTooltip(Tooltip.create(Component.translatable(
                "gui.svc_group_management.kick",
                state.getName()
        )));
        svcGroupManagement$kickButton.extractRenderState(guiGraphics, mouseX, mouseY, delta);
    }
}
