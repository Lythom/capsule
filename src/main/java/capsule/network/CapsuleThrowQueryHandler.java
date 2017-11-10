package capsule.network;

import capsule.client.CapsulePreviewHandler;
import capsule.items.CapsuleItem;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * The MessageHandlerOnServer is used to process the network message once it has
 * arrived on the Server side. WARNING! In 1.8 the MessageHandler now runs in
 * its own thread. This means that if your onMessage code calls any vanilla
 * objects, it may cause crashes or subtle problems that are hard to reproduce.
 * Your onMessage handler should create a task which is later executed by the
 * client or server thread as appropriate - see below. User: The Grey Ghost
 * Date: 15/01/2015
 */
public class CapsuleThrowQueryHandler
        implements IMessageHandler<CapsuleThrowQueryToServer, IMessage> {

    protected static final Logger LOGGER = LogManager.getLogger(CapsuleThrowQueryHandler.class);

    /**
     * Called when a message is received of the appropriate type. CALLED BY THE
     * NETWORK THREAD
     *
     * @param message The message
     */
    public IMessage onMessage(final CapsuleThrowQueryToServer message,
                              MessageContext ctx) {
        if (ctx.side != Side.SERVER) {
            LOGGER.error("CapsuleThrowQueryToServer received on wrong side:" + ctx.side);
            return null;
        }
        if (!message.isMessageValid()) {
            LOGGER.error("CapsuleThrowQueryToServer was invalid" + message.toString());
            return null;
        }

        // we know for sure that this handler is only used on the server side,
        // so it is ok to assume
        // that the ctx handler is a serverhandler, and that WorldServer exists.
        // Packets received on the client side must be handled differently! See
        // MessageHandlerOnClient
        final EntityPlayerMP sendingPlayer = ctx.getServerHandler().playerEntity;
        if (sendingPlayer == null) {
            LOGGER.error("EntityPlayerMP was null when CapsuleThrowQueryToServer was received");
            return null;
        }

        // Execute the action on the main server thread by adding it as a scheduled task
        sendingPlayer.getServerWorld().addScheduledTask(() -> {
            // read the content of the template and send it back to the client
            ItemStack heldItem = sendingPlayer.getHeldItemMainhand();
            CapsuleItem.throwCapsule(heldItem, sendingPlayer, message.getPos());
        });

        return null;

    }
}