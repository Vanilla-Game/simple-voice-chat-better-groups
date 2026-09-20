package ru.vanillagame.voicechat.bettergroups.client.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import java.util.UUID;

// Version 1, pinned by server GroupMuteProtocol golden vectors.
public record MuteRequestPayload(int requestId, UUID group, boolean muted) implements CustomPacketPayload {
    public static final Type<MuteRequestPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("svc_better_groups", "pause_request"));
    public static final StreamCodec<RegistryFriendlyByteBuf, MuteRequestPayload> CODEC = new StreamCodec<>() {
        public MuteRequestPayload decode(RegistryFriendlyByteBuf b) {
            if (b.readUnsignedByte() != 1) throw new IllegalArgumentException("Unsupported mute protocol");
            int request = b.readInt();
            UUID group = b.readBoolean() ? new UUID(b.readLong(), b.readLong()) : null;
            return new MuteRequestPayload(request, group, b.readBoolean());
        }
        public void encode(RegistryFriendlyByteBuf b, MuteRequestPayload p) {
            b.writeByte(1); b.writeInt(p.requestId()); b.writeBoolean(p.group() != null);
            if (p.group() != null) { b.writeLong(p.group().getMostSignificantBits()); b.writeLong(p.group().getLeastSignificantBits()); }
            b.writeBoolean(p.muted());
        }
    };
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
