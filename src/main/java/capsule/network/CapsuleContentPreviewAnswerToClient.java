package capsule.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

import java.util.ArrayList;
import java.util.List;

/**
 * This Network Message is sent from the client to the server
 */
public class CapsuleContentPreviewAnswerToClient implements IMessage {

    private List<BlockPos> blockPositions = null;


    private String structureName = null;


    public CapsuleContentPreviewAnswerToClient(List<BlockPos> blockPositions, String structureName) {
        this.blockPositions = blockPositions;
        this.structureName = structureName;
    }

    // for use by the message handler only.
    public CapsuleContentPreviewAnswerToClient() {

    }

    /**
     * Called by the network code once it has received the message bytes over
     * the network. Used to read the ByteBuf contents into your member variables
     *
     * @param buf
     */
    @Override
    public void fromBytes(ByteBuf buf) {
        try {
            // these methods may also be of use for your code:
            // for Itemstacks - ByteBufUtils.readItemStack()
            // for NBT tags ByteBufUtils.readTag();
            // for Strings: ByteBufUtils.readUTF8String();
            int size = buf.readShort();
            this.blockPositions = new ArrayList<BlockPos>(size);
            for (int i = 0; i < size; i++) {
                this.blockPositions.add(BlockPos.fromLong(buf.readLong()));
            }
            this.structureName = ByteBufUtils.readUTF8String(buf);

        } catch (IndexOutOfBoundsException ioe) {
            System.err.println("Exception while reading CapsuleContentPreviewMessageToClient: " + ioe);
            return;
        }
    }

    /**
     * Called by the network code. Used to write the contents of your message
     * member variables into the ByteBuf, ready for transmission over the
     * network.
     *
     * @param buf
     */
    @Override
    public void toBytes(ByteBuf buf) {

        // these methods may also be of use for your code:
        // for Itemstacks - ByteBufUtils.writeItemStack()
        // for NBT tags ByteBufUtils.writeTag();
        // for Strings: ByteBufUtils.writeUTF8String();
        buf.writeShort(this.blockPositions.size());
        for (BlockPos pos : this.blockPositions) {
            buf.writeLong(pos.toLong());
        }
        ByteBufUtils.writeUTF8String(buf, this.structureName);
    }

    @Override
    public String toString() {
        return "CapsuleContentPreviewMessageToClient";
    }

    public List<BlockPos> getBlockPositions() {
        return blockPositions;
    }

    public String getStructureName() {
        return structureName;
    }

}