package ru.vanillagame.voicechat.bettergroups.client;

import com.mojang.blaze3d.platform.InputConstants;
import de.maxhenkel.voicechat.intercompatibility.ClientCompatibilityManager;
import de.maxhenkel.voicechat.voice.client.ClientManager;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import ru.vanillagame.voicechat.bettergroups.client.network.MuteStatePayload;

import java.util.UUID;

public final class GroupMuteClient {
    private static boolean supported;
    private static UUID pausedGroup;
    private static UUID requestedGroup;
    private static final MuteRequestTracker REQUESTS = new MuteRequestTracker();
    private static int waitingTicks;

    private GroupMuteClient() {}

    public static void initialize() {
        KeyMapping toggleKey = ClientCompatibilityManager.INSTANCE.registerKeyBinding(new KeyMapping(
                "key.svc_better_groups.toggle_group_mute", InputConstants.UNKNOWN.getValue(),
                KeyMapping.Category.register(Identifier.fromNamespaceAndPath(BetterGroupsClient.MOD_ID, "group"))));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (REQUESTS.isPending() && ++waitingTicks == 100) message("mute_timeout");
            while (toggleKey.consumeClick()) toggle();
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> reset());
        ClientCompatibilityManager.INSTANCE.onVoiceChatDisconnected(() ->
                Minecraft.getInstance().execute(GroupMuteClient::clearLocal));
    }

    public static void toggle() {
        if (!supported) { message("mute_unsupported"); return; }
        if (!REQUESTS.isPending()) requestedGroup = pausedGroup != null ? pausedGroup : currentGroup();
        if (requestedGroup == null) { message("no_group"); return; }
        // A repeated press retries the same operation. It cannot accidentally undo
        // a leave that succeeded on the server but whose confirmation was lost.
        int request = REQUESTS.begin(isMuted());
        waitingTicks = 0;
        if (!ClientNetworking.sendMuteRequest(request, requestedGroup, REQUESTS.targetMuted())) {
            unavailable();
        } else message("mute_pending");
    }

    public static void receive(MuteStatePayload payload) {
        if (payload.requestId() == 0) {
            supported = true;
            REQUESTS.clear();
            requestedGroup = null;
            pausedGroup = payload.group();
        } else if (!supported) {
            return; // Ignore acknowledgements from a channel that has disappeared.
        } else if (REQUESTS.acknowledge(payload.requestId())) {
            pausedGroup = payload.group();
            requestedGroup = null;
            message(payload.result() != 0 ? "mute_rejected" : isMuted() ? "group_muted" : "group_unmuted");
        } else if (payload.requestId() == -1 && !REQUESTS.isPending()) {
            pausedGroup = payload.group();
        }
        // Native group membership packets may arrive before or after this snapshot.
        // In particular, a null native group is the expected paused state.
    }

    public static boolean isTransmissionBlocked() { return REQUESTS.isBlocked(); }
    public static boolean isMuted() { return pausedGroup != null; }
    public static boolean isPending() { return REQUESTS.isPending(); }

    public static void unavailable() {
        boolean interrupted = REQUESTS.isPending();
        supported = false;
        // There can be no acknowledgement while the channel is unavailable.
        // Resume normal SVC transmission according to the actual group membership.
        clearLocal();
        if (interrupted) message("mute_interrupted");
    }

    public static void reset() {
        supported = false;
        clearLocal();
    }

    private static void clearLocal() {
        pausedGroup = null;
        requestedGroup = null;
        REQUESTS.clear();
        waitingTicks = 0;
    }

    private static void message(String key) {
        if (Minecraft.getInstance().player != null) {
            ScreenNavigation.showActionBar(Minecraft.getInstance(),
                    Component.translatable("message.svc_better_groups." + key));
        }
    }

    private static UUID currentGroup() {
        var manager = ClientManager.getPlayerStateManager();
        return Minecraft.getInstance().player == null || manager.isDisconnected() ? null : manager.getGroupID();
    }
}
