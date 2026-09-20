package ru.vanillagame.voicechat.bettergroups.client.mixin;

import de.maxhenkel.voicechat.gui.VoiceChatScreenBase;
import de.maxhenkel.voicechat.gui.group.JoinGroupScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.vanillagame.voicechat.bettergroups.client.GroupMuteClient;

@Mixin(value = JoinGroupScreen.class, remap = false)
public abstract class JoinGroupScreenMixin extends VoiceChatScreenBase {
    @Shadow protected Button createGroup;
    @Unique private Button svcBetterGroups$return;

    protected JoinGroupScreenMixin() { super(Component.empty(), 0, 0); }

    @Inject(method = "init", at = @At("RETURN"), remap = false)
    private void svcBetterGroups$addReturn(CallbackInfo ci) {
        svcBetterGroups$return = Button.builder(Component.translatable("gui.svc_better_groups.unmute_group"),
                        button -> GroupMuteClient.toggle())
                .bounds(guiLeft + xSize / 2 + 2, guiTop + ySize - 27, (xSize - 18) / 2, 20).build();
        addRenderableWidget(svcBetterGroups$return);
        svcBetterGroups$updateReturn();
    }

    // JoinGroupScreen inherits tick; override it to handle a pause acknowledgement
    // arriving after SVC has already opened this screen.
    @Override
    public void tick() {
        super.tick();
        svcBetterGroups$updateReturn();
    }

    @Unique
    private void svcBetterGroups$updateReturn() {
        if (svcBetterGroups$return == null) return;
        boolean paused = GroupMuteClient.isMuted() || GroupMuteClient.isPending();
        svcBetterGroups$return.setMessage(Component.translatable(GroupMuteClient.isPending()
                ? "gui.svc_better_groups.retry_group" : "gui.svc_better_groups.unmute_group"));
        svcBetterGroups$return.visible = paused;
        svcBetterGroups$return.active = paused;
        createGroup.setWidth(paused ? (xSize - 18) / 2 : xSize - 14);
    }
}
