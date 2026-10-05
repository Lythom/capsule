package capsule.network;

import capsule.CapsuleMod;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record CapsuleThrowQueryToServer(boolean instant, BlockPos pos) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<CapsuleThrowQueryToServer> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(CapsuleMod.MODID, "throw_query"));

    public static final StreamCodec<FriendlyByteBuf, CapsuleThrowQueryToServer> STREAM_CODEC =
            StreamCodec.of(CapsuleThrowQueryToServer::encode, CapsuleThrowQueryToServer::decode);

    public CapsuleThrowQueryToServer(BlockPos pos, boolean instant) {
        this(instant, pos);
    }

    private static CapsuleThrowQueryToServer decode(FriendlyByteBuf buf) {
        boolean instant = buf.readBoolean();
        boolean hasPos = buf.readBoolean();
        BlockPos pos = null;
        if (hasPos) {
            pos = BlockPos.of(buf.readLong());
        }
        return new CapsuleThrowQueryToServer(instant, pos);
    }

    private static void encode(FriendlyByteBuf buf, CapsuleThrowQueryToServer msg) {
        buf.writeBoolean(msg.instant);
        boolean hasPos = msg.pos != null;
        buf.writeBoolean(hasPos);
        if (hasPos) {
            buf.writeLong(msg.pos.asLong());
        }
    }

    @Override
    public CustomPacketPayload.Type<CapsuleThrowQueryToServer> type() {
        return TYPE;
    }

    @Override
    public String toString() {
        return getClass().toString();
    }
}
