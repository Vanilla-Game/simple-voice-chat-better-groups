package ru.vanillagame.voicechat.bettergroups.client.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

// Wire format: [selected version u8]. This message only confirms protocol
// negotiation; group membership is sent separately on group_state.
public record ServerHelloPayload(int protocolVersion) implements CustomPacketPayload {

    public static final Type<ServerHelloPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath("svc_better_groups", "server_hello")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, ServerHelloPayload> CODEC =
            new StreamCodec<>() {
                @Override
                public ServerHelloPayload decode(RegistryFriendlyByteBuf buffer) {
                    return new ServerHelloPayload(buffer.readUnsignedByte());
                }

                @Override
                public void encode(RegistryFriendlyByteBuf buffer, ServerHelloPayload payload) {
                    buffer.writeByte(payload.protocolVersion());
                }
            };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
