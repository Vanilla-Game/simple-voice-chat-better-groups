package ru.vanillagame.voicechat.groupmanagement.client.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.UUID;

// Wire format: [version u8][flags u8][group uuid][leader uuid], flags bit 0 =
// group present, bit 1 = leader present, uuids as two big-endian longs. The
// byte layout is the compatibility contract with the server plugin, pinned by
// the golden vectors in the server-side LeaderSyncProtocolTest; change it only
// together with those vectors and a protocol version bump.
public record LeaderStatePayload(int protocolVersion, UUID groupId, UUID leaderId)
        implements CustomPacketPayload {

    public static final Type<LeaderStatePayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath("svc_group_management", "leader_state")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, LeaderStatePayload> CODEC =
            new StreamCodec<>() {
                @Override
                public LeaderStatePayload decode(RegistryFriendlyByteBuf buffer) {
                    int protocolVersion = buffer.readUnsignedByte();
                    int flags = buffer.readUnsignedByte();
                    UUID groupId = (flags & 1) == 0 ? null : readUuid(buffer);
                    UUID leaderId = (flags & 2) == 0 ? null : readUuid(buffer);
                    return new LeaderStatePayload(protocolVersion, groupId, leaderId);
                }

                @Override
                public void encode(RegistryFriendlyByteBuf buffer, LeaderStatePayload payload) {
                    int flags = 0;
                    if (payload.groupId() != null) {
                        flags |= 1;
                    }
                    if (payload.leaderId() != null) {
                        flags |= 2;
                    }
                    buffer.writeByte(payload.protocolVersion());
                    buffer.writeByte(flags);
                    if (payload.groupId() != null) {
                        writeUuid(buffer, payload.groupId());
                    }
                    if (payload.leaderId() != null) {
                        writeUuid(buffer, payload.leaderId());
                    }
                }
            };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static UUID readUuid(RegistryFriendlyByteBuf buffer) {
        return new UUID(buffer.readLong(), buffer.readLong());
    }

    private static void writeUuid(RegistryFriendlyByteBuf buffer, UUID uuid) {
        buffer.writeLong(uuid.getMostSignificantBits());
        buffer.writeLong(uuid.getLeastSignificantBits());
    }
}
