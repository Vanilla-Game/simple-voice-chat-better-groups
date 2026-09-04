package ru.vanillagame.voicechat.bettergroups.client.mixin;

import de.maxhenkel.voicechat.gui.VoiceChatScreenBase;
import de.maxhenkel.voicechat.gui.group.GroupScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.vanillagame.voicechat.bettergroups.client.ScreenNavigation;
import ru.vanillagame.voicechat.bettergroups.client.gui.InvitePlayerScreen;

@Mixin(value = GroupScreen.class, remap = false)
public abstract class GroupScreenMixin extends VoiceChatScreenBase {

    protected GroupScreenMixin() {
        super(Component.empty(), 0, 0);
    }

    @Inject(method = "init", at = @At("RETURN"), remap = false)
    private void svcBetterGroups$addInviteButton(CallbackInfo callbackInfo) {
        int buttonY = guiTop + ySize - 27;
        Button inviteButton = Button.builder(Component.literal("+"), button -> {
                    ScreenNavigation.setScreen(minecraft, new InvitePlayerScreen());
                })
                .bounds(guiLeft + 76, buttonY, 20, 20)
                .tooltip(Tooltip.create(Component.translatable("gui.svc_better_groups.invite")))
                .build();
        addRenderableWidget(inviteButton);
    }
}
