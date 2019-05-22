package capsule.network;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * This Network Message is sent from the client to the server
 */
public class LabelEditedMessageToServer implements IMessage {

    protected static final Logger LOGGER = LogManager.getLogger(LabelEditedMessageToServer.class);

    private String label;
    private boolean messageIsValid;

    public LabelEditedMessageToServer(String newLabel) {
        setLabel(newLabel);
        messageIsValid = true;
    }

    // for use by the message handler only.
    public LabelEditedMessageToServer() {
        messageIsValid = false;
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
            this.setLabel(ByteBufUtils.readUTF8String(buf));

            // these methods may also be of use for your code:
            // for Itemstacks - ByteBufUtils.readItemStack()
            // for NBT tags ByteBufUtils.readTag();
            // for Strings: ByteBufUtils.readUTF8String();

        } catch (IndexOutOfBoundsException ioe) {
            LOGGER.error("Exception while reading CapsuleLabelEditedMessageToClient: " + ioe);
            return;
        }
        messageIsValid = true;
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
        if (!messageIsValid)
            return;
        ByteBufUtils.writeUTF8String(buf, this.getLabel());

        // these methods may also be of use for your code:
        // for Itemstacks - ByteBufUtils.writeItemStack()
        // for NBT tags ByteBufUtils.writeTag();
        // for Strings: ByteBufUtils.writeUTF8String();
    }

    @Override
    public String toString() {
        return getClass().toString() + "[label=" + String.valueOf(getLabel()) + "]";
    }

    public boolean isMessageValid() {
        return messageIsValid;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

}