package ru.vanillagame.voicechat.grouptools.client.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record HelloPayload(int protocolVersion) implements CustomPacketPayload {

    public static final Type<HelloPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath("voicechat_group_tools", "hello")
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
