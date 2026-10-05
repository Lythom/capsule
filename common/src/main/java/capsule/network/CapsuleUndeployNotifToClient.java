package capsule.network;

import capsule.CapsuleMod;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record CapsuleUndeployNotifToClient(BlockPos posFrom, BlockPos posTo, int size, String templateName) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<CapsuleUndeployNotifToClient> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(CapsuleMod.MODID, "undeploy_notif"));

    public static final StreamCodec<FriendlyByteBuf, CapsuleUndeployNotifToClient> STREAM_CODEC =
            StreamCodec.of(CapsuleUndeployNotifToClient::encode, CapsuleUndeployNotifToClient::decode);

    private static CapsuleUndeployNotifToClient decode(FriendlyByteBuf buf) {
        return new CapsuleUndeployNotifToClient(buf.readBlockPos(), buf.readBlockPos(), buf.readShort(), buf.readUtf());
    }

    private static void encode(FriendlyByteBuf buf, CapsuleUndeployNotifToClient msg) {
        buf.writeBlockPos(msg.posFrom);
        buf.writeBlockPos(msg.posTo);
        buf.writeShort(msg.size);
        buf.writeUtf(msg.templateName == null ? "" : msg.templateName);
    }

    @Override
    public CustomPacketPayload.Type<CapsuleUndeployNotifToClient> type() {
        return TYPE;
    }

    @Override
    public String toString() {
        return getClass().toString();
    }

}
