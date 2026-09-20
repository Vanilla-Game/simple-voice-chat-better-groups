package ru.vanillagame.voicechat.bettergroups.client.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import java.util.UUID;

/** group is the saved return destination, or null when the player is not paused. */
public record MuteStatePayload(int requestId, int result, UUID group) implements CustomPacketPayload {
    public static final Type<MuteStatePayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("svc_better_groups", "pause_state"));
    public static final StreamCodec<RegistryFriendlyByteBuf, MuteStatePayload> CODEC = new StreamCodec<>() {
        public MuteStatePayload decode(RegistryFriendlyByteBuf b) {
            if (b.readUnsignedByte() != 1) throw new IllegalArgumentException("Unsupported pause protocol");
            int request = b.readInt();
            int result = b.readUnsignedByte();
            int present = b.readUnsignedByte();
            if (request < -1 || result > 1 || present > 1 || b.readableBytes() != present * 16)
                throw new IllegalArgumentException("Invalid pause snapshot");
            UUID group = present == 1 ? new UUID(b.readLong(), b.readLong()) : null;
            return new MuteStatePayload(request, result, group);
        }
        public void encode(RegistryFriendlyByteBuf b, MuteStatePayload p) {
            b.writeByte(1); b.writeInt(p.requestId()); b.writeByte(p.result()); b.writeBoolean(p.group() != null);
            if (p.group() != null) { b.writeLong(p.group().getMostSignificantBits()); b.writeLong(p.group().getLeastSignificantBits()); }
        }
    };
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
