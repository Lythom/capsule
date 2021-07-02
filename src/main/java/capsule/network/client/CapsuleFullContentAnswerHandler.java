package capsule.network.client;

import capsule.network.CapsuleFullContentAnswerToClient;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class CapsuleFullContentAnswerHandler implements IMessageHandler<CapsuleFullContentAnswerToClient, IMessage> {

    protected static final Logger LOGGER = LogManager.getLogger(CapsuleFullContentAnswerHandler.class);
    /**
     * Called when a message is received of the appropriate type.
     * CALLED BY THE NETWORK THREAD
     *
     * @param message The message
     */
    public IMessage onMessage(final CapsuleFullContentAnswerToClient message, MessageContext ctx) {
        if (ctx.side != Side.CLIENT) {
            LOGGER.error("CapsuleFullContentAnswerToClient received on wrong side:" + ctx.side);
            return null;
        }

        if (message.getTemplate() != null) {
            synchronized (capsule.client.CapsulePreviewHandler.currentFullPreview) {
                capsule.client.CapsulePreviewHandler.currentFullPreview.put(message.getStructureName(), message.getTemplate());
            }
        }

        return null;
    }
}