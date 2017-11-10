package capsule.network;

import capsule.client.CapsulePreviewHandler;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * The MessageHandlerOnServer is used to process the network message once it has arrived on the Server side.
 * WARNING!  In 1.8 the MessageHandler now runs in its own thread.  This means that if your onMessage code
 * calls any vanilla objects, it may cause crashes or subtle problems that are hard to reproduce.
 * Your onMessage handler should create a task which is later executed by the client or server thread as
 * appropriate.
 */
public class CapsuleContentPreviewAnswerHandler implements IMessageHandler<CapsuleContentPreviewAnswerToClient, IMessage> {

    protected static final Logger LOGGER = LogManager.getLogger(CapsuleContentPreviewAnswerHandler.class);
    /**
     * Called when a message is received of the appropriate type.
     * CALLED BY THE NETWORK THREAD
     *
     * @param message The message
     */
    public IMessage onMessage(final CapsuleContentPreviewAnswerToClient message, MessageContext ctx) {
        if (ctx.side != Side.CLIENT) {
            LOGGER.error("CapsuleContentPreviewMessageToClient received on wrong side:" + ctx.side);
            return null;
        }

        synchronized (CapsulePreviewHandler.currentPreview) {
            CapsulePreviewHandler.currentPreview.put(message.getStructureName(), message.getBlockPositions());
        }

        return null;
    }
}