package ru.vanillagame.voicechat.bettergroups.client;

import net.fabricmc.api.ClientModInitializer;

public final class BetterGroupsClient implements ClientModInitializer {

    public static final String MOD_ID = "svc_better_groups_client";

    // Set by the Loom production compatibility task.
    private static final String COMPAT_CHECK_PROPERTY = "svc_better_groups.compat_check";

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
        ClassLoader loader = BetterGroupsClient.class.getClassLoader();
        try {
            Class.forName("de.maxhenkel.voicechat.gui.group.GroupScreen", false, loader);
            Class.forName("de.maxhenkel.voicechat.gui.group.GroupEntry", false, loader);
            Class.forName("de.maxhenkel.voicechat.gui.EnterPasswordScreen", false, loader);
            // Loading our screen classes resolves their Simple Voice Chat
            // superclasses, verifying the GUI base classes still exist.
            Class.forName("ru.vanillagame.voicechat.bettergroups.client.gui.InvitePlayerScreen", false, loader);
            Class.forName("ru.vanillagame.voicechat.bettergroups.client.gui.InvitePlayerList", false, loader);
            Class.forName("ru.vanillagame.voicechat.bettergroups.client.gui.InvitePlayerEntry", false, loader);
            System.out.println("[" + MOD_ID + "] compat check passed");
            // The production client has already initialized native audio. Skip
            // shutdown hooks so this test-only early exit cannot tear it down
            // from a partially initialized state.
            Runtime.getRuntime().halt(0);
        } catch (Throwable failure) {
            System.err.println("[" + MOD_ID + "] compat check failed");
            failure.printStackTrace();
            Runtime.getRuntime().halt(1);
        }
    }
}
