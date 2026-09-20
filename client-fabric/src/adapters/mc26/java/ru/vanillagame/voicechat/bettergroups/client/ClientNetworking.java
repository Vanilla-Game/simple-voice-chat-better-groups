package ru.vanillagame.voicechat.bettergroups.client;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ServerboundPlayChannelEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import ru.vanillagame.voicechat.bettergroups.client.network.ClientHelloPayload;
import ru.vanillagame.voicechat.bettergroups.client.network.MuteRequestPayload;
import ru.vanillagame.voicechat.bettergroups.client.network.MuteStatePayload;
import ru.vanillagame.voicechat.bettergroups.client.network.GroupStatePayload;
import ru.vanillagame.voicechat.bettergroups.client.network.ServerHelloPayload;

public final class ClientNetworking {

    private static final int PROTOCOL_VERSION = 2;

    private static boolean clientHelloSent;

    private ClientNetworking() {
    }

    public static void initialize() {
        PayloadTypeRegistry.serverboundPlay().register(MuteRequestPayload.TYPE, MuteRequestPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(MuteStatePayload.TYPE, MuteStatePayload.CODEC);
        ClientPlayNetworking.registerGlobalReceiver(MuteStatePayload.TYPE, (payload, context) ->
                context.client().execute(() -> GroupMuteClient.receive(payload)));
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            GroupMuteClient.reset();
            sendMuteHello();
        });
        ServerboundPlayChannelEvents.REGISTER.register((handler, sender, client, channels) -> {
            if (channels.contains(MuteRequestPayload.TYPE.id())) {
                GroupMuteClient.unavailable();
                sendMuteHello();
            }
        });
        ServerboundPlayChannelEvents.UNREGISTER.register((handler, sender, client, channels) -> {
            if (channels.contains(MuteRequestPayload.TYPE.id())) GroupMuteClient.unavailable();
        });

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

    private static void sendMuteHello() {
        if (ClientPlayNetworking.canSend(MuteRequestPayload.TYPE)) {
            ClientPlayNetworking.send(new MuteRequestPayload(0, null, false));
        }
    }

    public static boolean sendMuteRequest(int request, java.util.UUID group, boolean muted) {
        if (!ClientPlayNetworking.canSend(MuteRequestPayload.TYPE)) return false;
        ClientPlayNetworking.send(new MuteRequestPayload(request, group, muted));
        return true;
    }

    private static void sendClientHelloIfPossible() {
        if (!clientHelloSent && ClientPlayNetworking.canSend(ClientHelloPayload.TYPE)) {
            clientHelloSent = true;
            ClientPlayNetworking.send(new ClientHelloPayload(PROTOCOL_VERSION));
        }
    }
}
