package ru.vanillagame.voicechat.bettergroups.test;

import java.net.URI;
import java.nio.file.Path;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;

public final class VoiceChatCompatibilityGameTest implements FabricClientGameTest {

    @Override
    public void runTest(ClientGameTestContext context) {
        ClassLoader loader = VoiceChatCompatibilityGameTest.class.getClassLoader();

        try {
            verifyPackagedAddonSource();

            // Mixins are applied when their target classes are defined. With
            // "required": true and defaultRequire 1, a missing injection point
            // fails one of these class loads and therefore the game test.
            Class.forName("de.maxhenkel.voicechat.gui.group.GroupScreen", false, loader);
            Class.forName("de.maxhenkel.voicechat.gui.group.GroupEntry", false, loader);
            Class.forName("de.maxhenkel.voicechat.gui.EnterPasswordScreen", false, loader);

            // Loading our screens also resolves their Simple Voice Chat base
            // classes, verifying that those compatibility contracts still exist.
            Class.forName("ru.vanillagame.voicechat.bettergroups.client.gui.InvitePlayerScreen", false, loader);
            Class.forName("ru.vanillagame.voicechat.bettergroups.client.gui.InvitePlayerList", false, loader);
            Class.forName("ru.vanillagame.voicechat.bettergroups.client.gui.InvitePlayerEntry", false, loader);
            System.out.println("[svc_better_groups_client_test] compatibility game test passed");
        } catch (Throwable failure) {
            throw new AssertionError("Simple Voice Chat compatibility check failed", failure);
        }
    }

    private static void verifyPackagedAddonSource() throws Exception {
        String expectedJar = System.getProperty("svc.bettergroups.expectedJar");
        if (expectedJar == null || expectedJar.isBlank()) {
            throw new AssertionError("Missing svc.bettergroups.expectedJar; compatibility test is not using the packaged-JAR runner");
        }

        URI actualLocation = Class.forName(
            "ru.vanillagame.voicechat.bettergroups.client.BetterGroupsClient",
            false,
            VoiceChatCompatibilityGameTest.class.getClassLoader()
        ).getProtectionDomain().getCodeSource().getLocation().toURI();
        Path expected = Path.of(URI.create(expectedJar)).toRealPath();
        Path actual = Path.of(actualLocation).toRealPath();
        if (!actual.equals(expected)) {
            throw new AssertionError("Addon loaded from " + actual + " instead of packaged JAR " + expected);
        }
    }
}
