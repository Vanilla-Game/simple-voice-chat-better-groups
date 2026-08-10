package ru.vanillagame.voicechat.bettergroups.test;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;

public final class VoiceChatCompatibilityGameTest implements FabricClientGameTest {

    @Override
    public void runTest(ClientGameTestContext context) {
        ClassLoader loader = VoiceChatCompatibilityGameTest.class.getClassLoader();

        try {
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
}
