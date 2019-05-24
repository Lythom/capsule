package capsule.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * This Network Message is sent from the client to the server
 */
public class CapsuleContentPreviewAnswerToClient implements IMessage {

    protected static final Logger LOGGER = LogManager.getLogger(CapsuleContentPreviewAnswerToClient.class);

    private List<AxisAlignedBB> boundingBoxes = null;
    private String structureName = null;


    public CapsuleContentPreviewAnswerToClient(List<AxisAlignedBB> boundingBoxes, String structureName) {
        this.boundingBoxes = boundingBoxes;
        this.structureName = structureName;
    }

    // for use by the message handler only.
    public CapsuleContentPreviewAnswerToClient() {

    }

    /**
     * Called by the network code once it has received the message bytes over
     * the network. Used to read the ByteBuf contents into your member variables
     *
     * @param buf buffer content to read from
     */
    @Override
    public void fromBytes(ByteBuf buf) {
        try {
            // these methods may also be of use for your code:
            // for Itemstacks - ByteBufUtils.readItemStack()
            // for NBT tags ByteBufUtils.readTag();
            // for Strings: ByteBufUtils.readUTF8String();
            this.structureName = ByteBufUtils.readUTF8String(buf);
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

    /**
     * Called by the network code. Used to write the contents of your message
     * member variables into the ByteBuf, ready for transmission over the
     * network.
     *
     * @param buf buffer content to write into
     */
    @Override
    public void toBytes(ByteBuf buf) {

        // these methods may also be of use for your code:
        // for Itemstacks - ByteBufUtils.writeItemStack()
        // for NBT tags ByteBufUtils.writeTag();
        // for Strings: ByteBufUtils.writeUTF8String();
        ByteBufUtils.writeUTF8String(buf, this.structureName);
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