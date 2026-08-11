package ru.vanillagame.voicechat.bettergroups.client;

import net.fabricmc.api.ClientModInitializer;

public final class BetterGroupsClient implements ClientModInitializer {

    public static final String MOD_ID = "svc_better_groups_client";

    @Override
    public void onInitializeClient() {
        // Mixins provide the UI integration. Server-side commands remain authoritative.
        ClientNetworking.initialize();
    }
}
