package ru.vanillagame.voicechat.bettergroups.client;

import net.fabricmc.api.ClientModInitializer;

public final class BetterGroupsClient implements ClientModInitializer {

    public static final String MOD_ID = "svc_better_groups_client";

    @Override
    public void onInitializeClient() {
        // Group management uses server commands; group mute only changes local playback.
        ClientNetworking.initialize();
        GroupMuteClient.initialize();
    }
}
