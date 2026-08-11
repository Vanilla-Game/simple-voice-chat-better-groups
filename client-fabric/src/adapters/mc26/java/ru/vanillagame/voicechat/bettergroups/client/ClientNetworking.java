package ru.vanillagame.voicechat.bettergroups.client;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ServerboundPlayChannelEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import ru.vanillagame.voicechat.bettergroups.client.network.ClientHelloPayload;
import ru.vanillagame.voicechat.bettergroups.client.network.GroupStatePayload;
import ru.vanillagame.voicechat.bettergroups.client.network.ServerHelloPayload;

public final class ClientNetworking {

    private static final int PROTOCOL_VERSION = 2;

    private static boolean clientHelloSent;

    private ClientNetworking() {
    }

    public static void initialize() {
        PayloadTypeRegistry.serverboundPlay().register(ClientHelloPayload.TYPE, ClientHelloPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(ServerHelloPayload.TYPE, ServerHelloPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(GroupStatePayload.TYPE, GroupStatePayload.CODEC);

        ClientPlayNetworking.registerGlobalReceiver(ServerHelloPayload.TYPE, (payload, context) ->
                context.client().execute(() -> {
                    if (payload.protocolVersion() == PROTOCOL_VERSION) {
                        ServerSupport.confirm();
                    } else {
                        ServerSupport.clear();
                        GroupClientState.clear();
                    }
                })
        );
        ClientPlayNetworking.registerGlobalReceiver(GroupStatePayload.TYPE, (payload, context) ->
                context.client().execute(() -> {
                    if (payload.protocolVersion() == PROTOCOL_VERSION && ServerSupport.isAvailable()) {
                        GroupClientState.update(payload);
                    } else if (payload.protocolVersion() != PROTOCOL_VERSION) {
                        ServerSupport.clear();
                        GroupClientState.clear();
                    }
                })
        );

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) ->
                client.execute(() -> {
                    ServerSupport.clear();
                    GroupClientState.clear();
                    clientHelloSent = false;
                    sendClientHelloIfPossible();
                })
        );
        // A Bukkit server announces its plugin channels in a minecraft:register
        // packet that arrives after the JOIN event, so the JOIN-time attempt sees
        // canSend() == false there. A re-announcement also means the server-side
        // plugin was reloaded and lost its handshake state, so always re-send.
        ServerboundPlayChannelEvents.REGISTER.register((handler, sender, client, channels) -> {
            if (channels.contains(ClientHelloPayload.TYPE.id())) {
                client.execute(() -> {
                    ServerSupport.clear();
                    GroupClientState.clear();
                    clientHelloSent = false;
                    sendClientHelloIfPossible();
                });
            }
        });
        ServerboundPlayChannelEvents.UNREGISTER.register((handler, sender, client, channels) -> {
            if (channels.contains(ClientHelloPayload.TYPE.id())) {
                client.execute(() -> {
                    ServerSupport.clear();
                    clientHelloSent = false;
                    GroupClientState.clear();
                });
            }
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) ->
                client.execute(() -> {
                    ServerSupport.clear();
                    GroupClientState.clear();
                    clientHelloSent = false;
                })
        );
    }

    private static void sendClientHelloIfPossible() {
        if (!clientHelloSent && ClientPlayNetworking.canSend(ClientHelloPayload.TYPE)) {
            clientHelloSent = true;
            ClientPlayNetworking.send(new ClientHelloPayload(PROTOCOL_VERSION));
        }
    }
}
