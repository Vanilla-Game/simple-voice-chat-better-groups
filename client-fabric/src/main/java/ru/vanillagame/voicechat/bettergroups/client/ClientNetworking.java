package ru.vanillagame.voicechat.bettergroups.client;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ServerboundPlayChannelEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import ru.vanillagame.voicechat.bettergroups.client.network.HelloPayload;
import ru.vanillagame.voicechat.bettergroups.client.network.LeaderStatePayload;

public final class ClientNetworking {

    private static final int PROTOCOL_VERSION = 1;

    private static boolean helloSent;

    private ClientNetworking() {
    }

    public static void initialize() {
        PayloadTypeRegistry.serverboundPlay().register(HelloPayload.TYPE, HelloPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(LeaderStatePayload.TYPE, LeaderStatePayload.CODEC);

        ClientPlayNetworking.registerGlobalReceiver(LeaderStatePayload.TYPE, (payload, context) ->
                context.client().execute(() -> {
                    if (payload.protocolVersion() == PROTOCOL_VERSION) {
                        LeaderClientState.update(payload);
                    } else {
                        LeaderClientState.clear();
                    }
                })
        );

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) ->
                client.execute(() -> {
                    LeaderClientState.clear();
                    helloSent = false;
                    sendHelloIfPossible();
                })
        );
        // A Bukkit server announces its plugin channels in a minecraft:register
        // packet that arrives after the JOIN event, so the JOIN-time attempt sees
        // canSend() == false there. A re-announcement also means the server-side
        // plugin was reloaded and lost its handshake state, so always re-send.
        ServerboundPlayChannelEvents.REGISTER.register((handler, sender, client, channels) -> {
            if (channels.contains(HelloPayload.TYPE.id())) {
                client.execute(() -> {
                    helloSent = false;
                    sendHelloIfPossible();
                });
            }
        });
        ServerboundPlayChannelEvents.UNREGISTER.register((handler, sender, client, channels) -> {
            if (channels.contains(HelloPayload.TYPE.id())) {
                client.execute(() -> {
                    helloSent = false;
                    LeaderClientState.clear();
                });
            }
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) ->
                client.execute(() -> {
                    LeaderClientState.clear();
                    helloSent = false;
                })
        );
    }

    private static void sendHelloIfPossible() {
        if (!helloSent && ClientPlayNetworking.canSend(HelloPayload.TYPE)) {
            helloSent = true;
            ClientPlayNetworking.send(new HelloPayload(PROTOCOL_VERSION));
        }
    }
}
