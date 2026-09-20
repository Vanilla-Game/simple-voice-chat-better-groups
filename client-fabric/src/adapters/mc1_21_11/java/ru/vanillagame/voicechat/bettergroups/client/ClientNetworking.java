package ru.vanillagame.voicechat.bettergroups.client;

import net.fabricmc.fabric.api.client.networking.v1.C2SPlayChannelEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
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
        PayloadTypeRegistry.playC2S().register(MuteRequestPayload.TYPE, MuteRequestPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(MuteStatePayload.TYPE, MuteStatePayload.CODEC);
        ClientPlayNetworking.registerGlobalReceiver(MuteStatePayload.TYPE, (payload, context) ->
                context.client().execute(() -> GroupMuteClient.receive(payload)));
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            GroupMuteClient.reset();
            sendMuteHello();
        });
        C2SPlayChannelEvents.REGISTER.register((handler, sender, client, channels) -> {
            if (channels.contains(MuteRequestPayload.TYPE.id())) {
                GroupMuteClient.unavailable();
                sendMuteHello();
            }
        });
        C2SPlayChannelEvents.UNREGISTER.register((handler, sender, client, channels) -> {
            if (channels.contains(MuteRequestPayload.TYPE.id())) GroupMuteClient.unavailable();
        });

        PayloadTypeRegistry.playC2S().register(ClientHelloPayload.TYPE, ClientHelloPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(ServerHelloPayload.TYPE, ServerHelloPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(GroupStatePayload.TYPE, GroupStatePayload.CODEC);

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
        // Bukkit advertises plugin channels after JOIN. Re-send the hello on
        // every matching registration because a plugin reload loses the
        // server-side negotiation state.
        C2SPlayChannelEvents.REGISTER.register((handler, sender, client, channels) -> {
            if (channels.contains(ClientHelloPayload.TYPE.id())) {
                client.execute(() -> {
                    ServerSupport.clear();
                    GroupClientState.clear();
                    clientHelloSent = false;
                    sendClientHelloIfPossible();
                });
            }
        });
        C2SPlayChannelEvents.UNREGISTER.register((handler, sender, client, channels) -> {
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
