package ru.vanillagame.voicechat.groupmanagement.client;

import net.fabricmc.api.ClientModInitializer;

public final class SvcGroupManagementClient implements ClientModInitializer {

    public static final String MOD_ID = "svc_group_management_client";

    // Set by CI through the voicechatCompatCheck Gradle property.
    private static final String COMPAT_CHECK_PROPERTY = "svc_group_management.compat_check";

    @Override
    public void onInitializeClient() {
        // Mixins provide the UI integration. Server-side commands remain authoritative.
        ClientNetworking.initialize();
        if (Boolean.getBoolean(COMPAT_CHECK_PROPERTY)) {
            runCompatCheckAndExit();
        }
    }

    // Mixins are applied when the target class is defined by the loader, so
    // loading both targets is enough: with "required": true and defaultRequire 1
    // any missing injection point fails the load. Initialization is skipped so
    // no game or voice chat state is touched.
    private static void runCompatCheckAndExit() {
        ClassLoader loader = SvcGroupManagementClient.class.getClassLoader();
        try {
            Class.forName("de.maxhenkel.voicechat.gui.group.GroupScreen", false, loader);
            Class.forName("de.maxhenkel.voicechat.gui.group.GroupEntry", false, loader);
            Class.forName("de.maxhenkel.voicechat.gui.EnterPasswordScreen", false, loader);
            // Loading our screen classes resolves their Simple Voice Chat
            // superclasses, verifying the GUI base classes still exist.
            Class.forName("ru.vanillagame.voicechat.groupmanagement.client.gui.InvitePlayerScreen", false, loader);
            Class.forName("ru.vanillagame.voicechat.groupmanagement.client.gui.InvitePlayerList", false, loader);
            Class.forName("ru.vanillagame.voicechat.groupmanagement.client.gui.InvitePlayerEntry", false, loader);
            System.out.println("[" + MOD_ID + "] compat check passed");
            System.exit(0);
        } catch (Throwable failure) {
            System.err.println("[" + MOD_ID + "] compat check failed");
            failure.printStackTrace();
            System.exit(1);
        }
    }
}
