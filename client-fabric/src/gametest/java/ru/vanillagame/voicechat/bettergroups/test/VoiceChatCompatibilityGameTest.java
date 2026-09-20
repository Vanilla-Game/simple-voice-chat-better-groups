package ru.vanillagame.voicechat.bettergroups.test;

import java.net.URI;
import java.nio.file.Path;
import java.util.UUID;
import java.util.HexFormat;
import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
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
            Class.forName("de.maxhenkel.voicechat.gui.group.JoinGroupScreen", false, loader);
            Class.forName("de.maxhenkel.voicechat.gui.group.GroupEntry", false, loader);
            Class.forName("de.maxhenkel.voicechat.gui.EnterPasswordScreen", false, loader);
            Class.forName("de.maxhenkel.voicechat.voice.client.MicThread", false, loader);
            Class.forName("de.maxhenkel.voicechat.voice.client.RenderEvents", false, loader);

            // Loading our screens also resolves their Simple Voice Chat base
            // classes, verifying that those compatibility contracts still exist.
            Class.forName("ru.vanillagame.voicechat.bettergroups.client.gui.InvitePlayerScreen", false, loader);
            Class.forName("ru.vanillagame.voicechat.bettergroups.client.gui.InvitePlayerList", false, loader);
            Class.forName("ru.vanillagame.voicechat.bettergroups.client.gui.InvitePlayerEntry", false, loader);
            verifyMuteProtocolCodecs();
            verifyReturnButton(context);
            verifyPauseChannelLoss(context);
            System.out.println("[svc_better_groups_client_test] compatibility game test passed");
        } catch (Throwable failure) {
            throw new AssertionError("Simple Voice Chat compatibility check failed", failure);
        }
    }

    private static void verifyReturnButton(ClientGameTestContext context) throws Exception {
        Class<?> client = Class.forName("ru.vanillagame.voicechat.bettergroups.client.GroupMuteClient");
        Class<?> payload = Class.forName("ru.vanillagame.voicechat.bettergroups.client.network.MuteStatePayload");
        var screen = context.computeOnClient(mc -> new de.maxhenkel.voicechat.gui.group.JoinGroupScreen());
        context.runOnClient(mc -> client.getMethod("reset").invoke(null));
        context.setScreen(() -> screen);
        context.runOnClient(mc -> {
            String label = net.minecraft.network.chat.Component.translatable("gui.svc_better_groups.unmute_group").getString();
            var button = screen.children().stream()
                    .filter(child -> child instanceof net.minecraft.client.gui.components.Button b && b.getMessage().getString().equals(label))
                    .map(child -> (net.minecraft.client.gui.components.Button) child).findFirst().orElseThrow();
            if (button.visible) throw new AssertionError("Return must be hidden without a server grant");
            var requestsField = client.getDeclaredField("REQUESTS");
            requestsField.setAccessible(true);
            Object requests = requestsField.get(null);
            requests.getClass().getMethod("begin", boolean.class).invoke(requests, false);
            screen.tick();
            if (!button.visible || !button.active) throw new AssertionError("Pending leave needs a Retry button even before acknowledgement");
            Object state = payload.getConstructor(int.class, int.class, UUID.class).newInstance(0, 0, UUID.randomUUID());
            client.getMethod("receive", payload).invoke(null, state);
            screen.tick();
            if (!button.visible || !button.active) throw new AssertionError("Late pause acknowledgement did not show Return");
            for (var child : screen.children()) {
                if (child instanceof net.minecraft.client.gui.components.Button other && other != button && other.visible
                        && other.getX() < button.getX() && other.getX() + other.getWidth() > button.getX())
                    throw new AssertionError("Create and Return buttons overlap");
            }
        });
        context.takeScreenshot("group-pause-return");
        context.runOnClient(mc -> {
            client.getMethod("reset").invoke(null);
            screen.tick();
            String label = net.minecraft.network.chat.Component.translatable("gui.svc_better_groups.unmute_group").getString();
            for (var child : screen.children()) {
                if (child instanceof net.minecraft.client.gui.components.Button button
                        && button.getMessage().getString().equals(label) && button.visible)
                    throw new AssertionError("Return remained visible after resetting the grant");
            }
        });
        context.setScreen(net.minecraft.client.gui.screens.TitleScreen::new);
    }

    private static void verifyPauseChannelLoss(ClientGameTestContext context) throws Exception {
        Class<?> client = Class.forName("ru.vanillagame.voicechat.bettergroups.client.GroupMuteClient");
        Class<?> payload = Class.forName("ru.vanillagame.voicechat.bettergroups.client.network.MuteStatePayload");
        context.runOnClient(mc -> {
            var requestsField = client.getDeclaredField("REQUESTS");
            requestsField.setAccessible(true);
            Object requests = requestsField.get(null);
            var destination = client.getDeclaredField("requestedGroup");
            destination.setAccessible(true);
            var ticks = client.getDeclaredField("waitingTicks");
            ticks.setAccessible(true);
            var supported = client.getDeclaredField("supported");
            supported.setAccessible(true);
            UUID group = UUID.randomUUID();
            for (boolean returning : new boolean[]{false, true}) {
                client.getMethod("reset").invoke(null);
                Object hello = payload.getConstructor(int.class, int.class, UUID.class)
                        .newInstance(0, 0, returning ? group : null);
                client.getMethod("receive", payload).invoke(null, hello);
                int request = (int) requests.getClass().getMethod("begin", boolean.class).invoke(requests, returning);
                destination.set(null, group);
                ticks.setInt(null, 50);
                if (!(boolean) client.getMethod("isTransmissionBlocked").invoke(null))
                    throw new AssertionError("Pending transition must initially block transmission");
                client.getMethod("unavailable").invoke(null);
                if ((boolean) client.getMethod("isTransmissionBlocked").invoke(null))
                    throw new AssertionError("Channel loss left the microphone blocked");
                if ((boolean) client.getMethod("isPending").invoke(null)
                        || (boolean) client.getMethod("isMuted").invoke(null)
                        || destination.get(null) != null || ticks.getInt(null) != 0)
                    throw new AssertionError("Channel loss retained stale pause state");
                client.getMethod("toggle").invoke(null);
                Object stale = payload.getConstructor(int.class, int.class, UUID.class)
                        .newInstance(request, 0, group);
                client.getMethod("receive", payload).invoke(null, stale);
                if (supported.getBoolean(null) || (boolean) client.getMethod("isTransmissionBlocked").invoke(null)
                        || (boolean) client.getMethod("isMuted").invoke(null))
                    throw new AssertionError("Unsupported toggle or stale acknowledgement restored the old transition");
                // Re-registering the channel negotiates a fresh authoritative snapshot.
                client.getMethod("receive", payload).invoke(null, hello);
                if (!supported.getBoolean(null) || (boolean) client.getMethod("isMuted").invoke(null) != returning)
                    throw new AssertionError("Fresh handshake did not restore server support");
            }
            client.getMethod("reset").invoke(null);
        });
    }

    @SuppressWarnings("unchecked")
    private static void verifyMuteProtocolCodecs() throws Exception {
        // Same golden vectors as the Bukkit protocol tests, using the packaged client codecs.
        Class<?> requestType = Class.forName("ru.vanillagame.voicechat.bettergroups.client.network.MuteRequestPayload");
        var requestCodec = (StreamCodec<RegistryFriendlyByteBuf, Object>) requestType.getField("CODEC").get(null);
        Object request = requestType.getConstructor(int.class, UUID.class, boolean.class).newInstance(
                42, UUID.fromString("00112233-4455-6677-8899-aabbccddeeff"), true);
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
        try {
            requestCodec.encode(buffer, request);
            byte[] bytes = new byte[buffer.readableBytes()];
            buffer.readBytes(bytes);
            if (!HexFormat.of().formatHex(bytes).equals("010000002a0100112233445566778899aabbccddeeff01")) {
                throw new AssertionError("Client mute request differs from the server protocol");
            }
            buffer.clear();
            String expected = "010000002a000100112233445566778899aabbccddeeff";
            buffer.writeBytes(HexFormat.of().parseHex(expected));
            Class<?> stateType = Class.forName("ru.vanillagame.voicechat.bettergroups.client.network.MuteStatePayload");
            var stateCodec = (StreamCodec<RegistryFriendlyByteBuf, Object>) stateType.getField("CODEC").get(null);
            Object state = stateCodec.decode(buffer);
            buffer.clear();
            stateCodec.encode(buffer, state);
            bytes = new byte[buffer.readableBytes()];
            buffer.readBytes(bytes);
            if (!HexFormat.of().formatHex(bytes).equals(expected)) {
                throw new AssertionError("Client mute state differs from the server protocol");
            }
        } finally {
            buffer.release();
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
