package ru.vanillagame.voicechat.groupmanagement.client.mixin;

import de.maxhenkel.voicechat.gui.EnterPasswordScreen;
import de.maxhenkel.voicechat.gui.VoiceChatScreenBase;
import de.maxhenkel.voicechat.voice.common.ClientGroup;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.vanillagame.voicechat.groupmanagement.client.Icons;
import ru.vanillagame.voicechat.groupmanagement.client.ServerSupport;

@Mixin(value = EnterPasswordScreen.class, remap = false)
public abstract class EnterPasswordScreenMixin extends VoiceChatScreenBase {

    @Unique
    private static final int SVC_GROUP_MANAGEMENT$BUTTON_SIZE = 20;

    @Unique
    private static final int SVC_GROUP_MANAGEMENT$BUTTON_GAP = 4;

    @Shadow
    private Button joinGroup;

    @Shadow
    private ClientGroup group;

    protected EnterPasswordScreenMixin() {
        super(Component.empty(), 0, 0);
    }

    @Inject(method = "init", at = @At("RETURN"), remap = false)
    private void svcGroupManagement$addRequestButton(CallbackInfo callbackInfo) {
        if (!ServerSupport.isAvailable()) {
            return;
        }

        joinGroup.setWidth(
                joinGroup.getWidth() - SVC_GROUP_MANAGEMENT$BUTTON_SIZE - SVC_GROUP_MANAGEMENT$BUTTON_GAP);
        Button requestButton = Button.builder(
                        Component.literal(Icons.AJAR_DOOR)
                                .withStyle(style -> style.withFont(Icons.FONT)),
                        button -> {
                    ServerSupport.requestJoin(group.getId());
                    minecraft.gui.setScreen(null);
                })
                .bounds(
                        guiLeft + xSize - 7 - SVC_GROUP_MANAGEMENT$BUTTON_SIZE,
                        guiTop + ySize - 20 - 7,
                        SVC_GROUP_MANAGEMENT$BUTTON_SIZE,
                        20
                )
                .tooltip(Tooltip.create(Component.translatable("gui.svc_group_management.request")))
                .build();
        addRenderableWidget(requestButton);
    }
}
