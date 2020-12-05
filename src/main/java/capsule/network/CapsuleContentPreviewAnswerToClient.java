package capsule.network;

import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
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

    private List<AxisAlignedBB> boundingBoxes = null;
    private String structureName = null;


    public CapsuleContentPreviewAnswerToClient(List<AxisAlignedBB> boundingBoxes, String structureName) {
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

    public CapsuleContentPreviewAnswerToClient(PacketBuffer buf) {
        try {
            this.structureName = buf.readString(32767);
            int size = buf.readShort();
            this.boundingBoxes = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                boolean isSingleBlock = buf.readBoolean();
                if (isSingleBlock) {
                    BlockPos p = BlockPos.fromLong(buf.readLong());
                    this.boundingBoxes.add(new AxisAlignedBB(p, p));
                } else {
                    this.boundingBoxes.add(new AxisAlignedBB(BlockPos.fromLong(buf.readLong()), BlockPos.fromLong(buf.readLong())));
                }
            }

        } catch (IndexOutOfBoundsException ioe) {
            LOGGER.error("Exception while reading CapsuleContentPreviewMessageToClient: " + ioe);
            return;
        }
    }

    public void toBytes(PacketBuffer buf) {
        buf.writeString(this.structureName);
        int size = Math.min(this.boundingBoxes.size(), Short.MAX_VALUE);
        buf.writeShort(size);
        for (int i = 0; i < size; i++) {
            AxisAlignedBB bb = boundingBoxes.get(i);
            boolean isSingleBlock = bb.getAverageEdgeLength() == 0;
            buf.writeBoolean(isSingleBlock);
            buf.writeLong(new BlockPos(bb.minX, bb.minY, bb.minZ).toLong());
            if (!isSingleBlock) {
                buf.writeLong(new BlockPos(bb.maxX, bb.maxY, bb.maxZ).toLong());
            }
        }
    }

    @Override
    public String toString() {
        return getClass().toString();
    }

    public List<AxisAlignedBB> getBoundingBoxes() {
        return boundingBoxes;
    }

    public String getStructureName() {
        return structureName;
    }

}