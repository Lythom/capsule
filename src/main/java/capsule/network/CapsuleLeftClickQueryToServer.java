package capsule.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

/**
 * This Network Message is sent from the client to the server
 */
public class CapsuleLeftClickQueryToServer implements IMessage {

    // for use by the message handler only.
    public CapsuleLeftClickQueryToServer() {

    }

    /**
     * Called by the network code once it has received the message bytes over
     * the network. Used to read the ByteBuf contents into your member variables
     *
     * @param buf
     */
    @Override
    public void fromBytes(ByteBuf buf) {
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
    }

    public boolean isMessageValid() {
        return true;
    }

    @Override
    public String toString() {
        return getClass().toString();
    }

}