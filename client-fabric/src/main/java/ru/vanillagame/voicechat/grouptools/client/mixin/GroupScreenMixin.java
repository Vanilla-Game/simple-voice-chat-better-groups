package ru.vanillagame.voicechat.grouptools.client.mixin;

import de.maxhenkel.voicechat.gui.VoiceChatScreenBase;
import de.maxhenkel.voicechat.gui.group.GroupScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.vanillagame.voicechat.grouptools.client.ServerSupport;

@Mixin(value = GroupScreen.class, remap = false)
public abstract class GroupScreenMixin extends VoiceChatScreenBase {

    @Unique
    private Button voiceChatGroupTools$inviteButton;

    protected GroupScreenMixin() {
        super(Component.empty(), 0, 0);
    }

    @Inject(method = "init", at = @At("RETURN"), remap = false)
    private void voiceChatGroupTools$addInviteButton(CallbackInfo callbackInfo) {
        int buttonY = guiTop + ySize - 27;
        voiceChatGroupTools$inviteButton = Button.builder(Component.literal("+"), button -> {
                    minecraft.gui.setScreen(new ChatScreen("/vcgroup invite ", false));
                })
                .bounds(guiLeft + 76, buttonY, 20, 20)
                .tooltip(Tooltip.create(Component.translatable("gui.voicechat_group_tools.invite")))
                .build();
        voiceChatGroupTools$inviteButton.visible = ServerSupport.isAvailable();
        addRenderableWidget(voiceChatGroupTools$inviteButton);
    }

    @Inject(method = "tick", at = @At("TAIL"), remap = false)
    private void voiceChatGroupTools$updateInviteButton(CallbackInfo callbackInfo) {
        if (voiceChatGroupTools$inviteButton != null) {
            voiceChatGroupTools$inviteButton.visible = ServerSupport.isAvailable();
        }
    }
}
