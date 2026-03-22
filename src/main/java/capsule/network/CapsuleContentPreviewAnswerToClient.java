package capsule.network;

import capsule.CapsuleMod;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * This Network Message is sent from the server to the client
 */
public record CapsuleContentPreviewAnswerToClient(String structureName, List<AABB> boundingBoxes) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<CapsuleContentPreviewAnswerToClient> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(CapsuleMod.MODID, "content_preview_answer"));

    protected static final Logger LOGGER = LogManager.getLogger(CapsuleContentPreviewAnswerToClient.class);

    public static final StreamCodec<FriendlyByteBuf, CapsuleContentPreviewAnswerToClient> STREAM_CODEC =
            StreamCodec.of(CapsuleContentPreviewAnswerToClient::encode, CapsuleContentPreviewAnswerToClient::decode);

    public CapsuleContentPreviewAnswerToClient(List<AABB> boundingBoxes, String structureName) {
        this(structureName, boundingBoxes);
    }

    private static CapsuleContentPreviewAnswerToClient decode(FriendlyByteBuf buf) {
        String structureName = buf.readUtf();
        int size = buf.readShort();
        List<AABB> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            boolean isSingleBlock = buf.readBoolean();
            if (isSingleBlock) {
                BlockPos p = BlockPos.of(buf.readLong());
                list.add(new AABB(p));
            } else {
                list.add(new AABB(Vec3.atLowerCornerOf(BlockPos.of(buf.readLong())), Vec3.atLowerCornerOf(BlockPos.of(buf.readLong()))));
            }
        }
        return new CapsuleContentPreviewAnswerToClient(structureName, list);
    }

    private static void encode(FriendlyByteBuf buf, CapsuleContentPreviewAnswerToClient msg) {
        buf.writeUtf(msg.structureName);
        int size = Math.min(msg.boundingBoxes.size(), Short.MAX_VALUE);
        buf.writeShort(size);
        for (int i = 0; i < size; i++) {
            AABB bb = msg.boundingBoxes.get(i);
            boolean isSingleBlock = bb.getSize() == 0;
            buf.writeBoolean(isSingleBlock);
            buf.writeLong(BlockPos.containing(bb.minX, bb.minY, bb.minZ).asLong());
            if (!isSingleBlock) {
                buf.writeLong(BlockPos.containing(bb.maxX, bb.maxY, bb.maxZ).asLong());
            }
        }
    }

    @Override
    public CustomPacketPayload.Type<CapsuleContentPreviewAnswerToClient> type() {
        return TYPE;
    }

    @Override
    public String toString() {
        return getClass().toString();
    }

}
