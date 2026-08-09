package ru.vanillagame.voicechat.groupmanagement.client.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

// Wire format: [version u8]. The byte layout is the compatibility contract
// with the server plugin, pinned by the golden vectors in the server-side
// LeaderSyncProtocolTest; change it only together with those vectors and a
// protocol version bump.
public record HelloPayload(int protocolVersion) implements CustomPacketPayload {

    public static final Type<HelloPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath("svc_group_management", "hello")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, HelloPayload> CODEC =
            new StreamCodec<>() {
                @Override
                public HelloPayload decode(RegistryFriendlyByteBuf buffer) {
                    return new HelloPayload(buffer.readUnsignedByte());
                }

                @Override
                public void encode(RegistryFriendlyByteBuf buffer, HelloPayload payload) {
                    buffer.writeByte(payload.protocolVersion());
                }
            };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
