package ru.vanillagame.voicechat.grouptools.client;

import net.fabricmc.api.ClientModInitializer;

public final class VoiceChatGroupToolsClient implements ClientModInitializer {

    public static final String MOD_ID = "voicechat_group_tools_client";

    @Override
    public void onInitializeClient() {
        // Mixins provide the UI integration. Server-side commands remain authoritative.
    }
}
