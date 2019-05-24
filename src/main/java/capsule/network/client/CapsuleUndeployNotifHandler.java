package capsule.network.client;

import capsule.helpers.Capsule;
import capsule.network.CapsuleUndeployNotifToClient;
import net.minecraft.client.Minecraft;
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
public class CapsuleUndeployNotifHandler implements IMessageHandler<CapsuleUndeployNotifToClient, IMessage> {

    protected static final Logger LOGGER = LogManager.getLogger(CapsuleUndeployNotifHandler.class);
    /**
     * Called when a message is received of the appropriate type.
     * CALLED BY THE NETWORK THREAD
     *
     * @param message The message
     */
    public IMessage onMessage(final CapsuleUndeployNotifToClient message, MessageContext ctx) {
        if (ctx.side != Side.CLIENT) {
            LOGGER.error("CapsuleContentPreviewMessageToClient received on wrong side:" + ctx.side);
            return null;
        }

        Capsule.showUndeployParticules(Minecraft.getMinecraft().world, message.posFrom, message.posTo, message.size);

        return null;
    }
}