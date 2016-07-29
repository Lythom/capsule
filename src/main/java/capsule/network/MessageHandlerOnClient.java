package capsule.network;
import capsule.CapsulePreviewHandler;
import capsule.items.CapsuleItem;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;

/**
 * The MessageHandlerOnServer is used to process the network message once it has arrived on the Server side.
 * WARNING!  In 1.8 the MessageHandler now runs in its own thread.  This means that if your onMessage code
 * calls any vanilla objects, it may cause crashes or subtle problems that are hard to reproduce.
 * Your onMessage handler should create a task which is later executed by the client or server thread as
 * appropriate - see below.
 * User: The Grey Ghost
 * Date: 15/01/2015
 */
public class MessageHandlerOnClient implements IMessageHandler<CapsuleContentPreviewMessageToClient, IMessage>
{
  /**
   * Called when a message is received of the appropriate type.
   * CALLED BY THE NETWORK THREAD
   * @param message The message
   */
  public IMessage onMessage(final CapsuleContentPreviewMessageToClient message, MessageContext ctx) {
    if (ctx.side != Side.CLIENT) {
      System.err.println("CapsuleContentPreviewMessageToClient received on wrong side:" + ctx.side);
      return null;
    }
    
    synchronized (CapsulePreviewHandler.currentPreview) {
    	CapsulePreviewHandler.currentPreview.put(message.getStructureName(), message.getBlockPositions());
	}

    return null;
  }
}