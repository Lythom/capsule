package capsule.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * This Network Message is sent from the client to the server
 */
public class CapsuleUndeployNotifToClient implements IMessage {

    protected static final Logger LOGGER = LogManager.getLogger(CapsuleUndeployNotifToClient.class);

    public BlockPos posFrom = null;
    public BlockPos posTo = null;
    public int size = 0;

    public CapsuleUndeployNotifToClient(BlockPos posFrom, BlockPos posTo, int size) {
        this.posFrom = posFrom;
        this.posTo = posTo;
        this.size = size;
    }

    // for use by the message handler only.
    public CapsuleUndeployNotifToClient() {
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        try {
            this.posFrom = BlockPos.fromLong(buf.readLong());
            this.posTo = BlockPos.fromLong(buf.readLong());
            this.size = buf.readShort();
        } catch (IndexOutOfBoundsException ioe) {
            LOGGER.error("Exception while reading CapsuleUndeployNotifToClient: " + ioe);
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeLong(posFrom.toLong());
        buf.writeLong(posTo.toLong());
        buf.writeShort(size);
    }

    @Override
    public String toString() {
        return getClass().toString();
    }

}