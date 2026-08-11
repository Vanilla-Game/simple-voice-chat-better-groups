package ru.vanillagame.voicechat.bettergroups.client.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

// Wire format: [version u8]. The byte layout is the compatibility contract
// with the server plugin, pinned by the golden vectors in the server-side
// GroupSyncProtocolTest; change it only together with those vectors and a
// protocol version bump.
public record ClientHelloPayload(int protocolVersion) implements CustomPacketPayload {

    public static final Type<ClientHelloPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath("svc_better_groups", "client_hello")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, ClientHelloPayload> CODEC =
            new StreamCodec<>() {
                @Override
                public ClientHelloPayload decode(RegistryFriendlyByteBuf buffer) {
                    return new ClientHelloPayload(buffer.readUnsignedByte());
                }

                @Override
                public void encode(RegistryFriendlyByteBuf buffer, ClientHelloPayload payload) {
                    buffer.writeByte(payload.protocolVersion());
                }
            };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
