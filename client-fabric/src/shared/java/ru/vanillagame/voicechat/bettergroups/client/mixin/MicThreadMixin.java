package ru.vanillagame.voicechat.bettergroups.client.mixin;

import de.maxhenkel.voicechat.voice.client.MicThread;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.vanillagame.voicechat.bettergroups.client.GroupMuteClient;

@Mixin(value = MicThread.class, remap = false)
public abstract class MicThreadMixin {
    @Inject(method = "sendAudioPacket", at = @At("HEAD"), cancellable = true, remap = false)
    private void svcBetterGroups$waitForServer(short[] audio, boolean whispering, CallbackInfo callback) {
        if (GroupMuteClient.isTransmissionBlocked()) callback.cancel();
    }
}
