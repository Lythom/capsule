package capsule.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.AABB;
import net.minecraft.core.BlockPos;
import net.minecraftforge.fml.network.NetworkEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * This Network Message is sent from the client to the server
 */
public class CapsuleContentPreviewAnswerToClient {

    protected static final Logger LOGGER = LogManager.getLogger(CapsuleContentPreviewAnswerToClient.class);

    private List<AABB> boundingBoxes = null;
    private String structureName = null;


    public CapsuleContentPreviewAnswerToClient(List<AABB> boundingBoxes, String structureName) {
        this.boundingBoxes = boundingBoxes;
        this.structureName = structureName;
    }

    public void onClient(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            synchronized (capsule.client.CapsulePreviewHandler.currentPreview) {
                capsule.client.CapsulePreviewHandler.currentPreview.put(getStructureName(), getBoundingBoxes());
            }
        });
        ctx.get().setPacketHandled(true);
    }

    public CapsuleContentPreviewAnswerToClient(FriendlyByteBuf buf) {
        try {
            this.structureName = buf.readUtf(32767);
            int size = buf.readShort();
            this.boundingBoxes = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                boolean isSingleBlock = buf.readBoolean();
                if (isSingleBlock) {
                    BlockPos p = BlockPos.of(buf.readLong());
                    this.boundingBoxes.add(new AABB(p, p));
                } else {
                    this.boundingBoxes.add(new AABB(BlockPos.of(buf.readLong()), BlockPos.of(buf.readLong())));
                }
            }

        } catch (IndexOutOfBoundsException ioe) {
            LOGGER.error("Exception while reading CapsuleContentPreviewMessageToClient: " + ioe);
            return;
        }
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUtf(this.structureName);
        int size = Math.min(this.boundingBoxes.size(), Short.MAX_VALUE);
        buf.writeShort(size);
        for (int i = 0; i < size; i++) {
            AABB bb = boundingBoxes.get(i);
            boolean isSingleBlock = bb.getSize() == 0;
            buf.writeBoolean(isSingleBlock);
            buf.writeLong(new BlockPos(bb.minX, bb.minY, bb.minZ).asLong());
            if (!isSingleBlock) {
                buf.writeLong(new BlockPos(bb.maxX, bb.maxY, bb.maxZ).asLong());
            }
        }
    }

    @Override
    public String toString() {
        return getClass().toString();
    }

    public List<AABB> getBoundingBoxes() {
        return boundingBoxes;
    }

    public String getStructureName() {
        return structureName;
    }

}