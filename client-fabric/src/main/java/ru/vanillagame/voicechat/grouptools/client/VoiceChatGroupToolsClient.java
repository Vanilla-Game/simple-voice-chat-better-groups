package ru.vanillagame.voicechat.grouptools.client;

import net.fabricmc.api.ClientModInitializer;

public final class VoiceChatGroupToolsClient implements ClientModInitializer {

    public static final String MOD_ID = "voicechat_group_tools_client";

    // Set by CI through the voicechatCompatCheck Gradle property.
    private static final String COMPAT_CHECK_PROPERTY = "voicechat_group_tools.compat_check";

    @Override
    public void onInitializeClient() {
        // Mixins provide the UI integration. Server-side commands remain authoritative.
        if (Boolean.getBoolean(COMPAT_CHECK_PROPERTY)) {
            runCompatCheckAndExit();
        }
    }

    // Mixins are applied when the target class is defined by the loader, so
    // loading both targets is enough: with "required": true and defaultRequire 1
    // any missing injection point fails the load. Initialization is skipped so
    // no game or voice chat state is touched.
    private static void runCompatCheckAndExit() {
        ClassLoader loader = VoiceChatGroupToolsClient.class.getClassLoader();
        try {
            Class.forName("de.maxhenkel.voicechat.gui.group.GroupScreen", false, loader);
            Class.forName("de.maxhenkel.voicechat.gui.group.GroupEntry", false, loader);
            System.out.println("[" + MOD_ID + "] compat check passed");
            System.exit(0);
        } catch (Throwable failure) {
            System.err.println("[" + MOD_ID + "] compat check failed");
            failure.printStackTrace();
            System.exit(1);
        }
    }
}
