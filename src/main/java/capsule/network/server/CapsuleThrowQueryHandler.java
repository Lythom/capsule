package capsule.network.server;

import capsule.CommonProxy;
import capsule.helpers.Capsule;
import capsule.items.CapsuleItem;
import capsule.network.CapsuleThrowQueryToServer;
import capsule.network.CapsuleUndeployNotifToClient;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.network.NetworkRegistry;
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
        final EntityPlayerMP sendingPlayer = ctx.getServerHandler().player;
        if (sendingPlayer == null) {
            LOGGER.error("EntityPlayerMP was null when CapsuleThrowQueryToServer was received");
            return null;
        }

        // Execute the action on the main server thread by adding it as a scheduled task
        WorldServer world = sendingPlayer.getServerWorld();
        world.addScheduledTask(() -> {
            ItemStack heldItem = sendingPlayer.getHeldItemMainhand();
            if (heldItem.getItem() instanceof CapsuleItem) {
                if (message.instant && message.pos != null) {
                    int size = CapsuleItem.getSize(heldItem);
                    int extendLength = (size - 1) / 2;
                    // instant capsule initial capture
                    if (heldItem.getItemDamage() == CapsuleItem.STATE_EMPTY) {
                        boolean captured = Capsule.captureAtPosition(heldItem, sendingPlayer.getName(), size, world, message.pos);
                        if (captured) {
                            BlockPos center = message.pos.add(0, size / 2, 0);
                            CommonProxy.simpleNetworkWrapper.sendToAllAround(
                                    new CapsuleUndeployNotifToClient(center, sendingPlayer.getPosition(), size),
                                    new NetworkRegistry.TargetPoint(sendingPlayer.dimension, center.getX(), center.getY(), center.getZ(), 200 + size)
                            );
                        }
                    }
                    // instant deployment
                    else {
                        boolean deployed = Capsule.deployCapsule(heldItem, message.pos.add(0, -1, 0), sendingPlayer.getName(), extendLength, world);
                        if (deployed) {
                            world.playSound(null, message.pos, SoundEvents.ENTITY_IRONGOLEM_ATTACK, SoundCategory.BLOCKS, 0.4F, 0.1F);
                            Capsule.showDeployParticules(world, message.pos, size);
                        }
                        if (deployed && CapsuleItem.isOneUse(heldItem)) {
                            heldItem.shrink(1);
                        }
                    }
                }
                if (!message.instant) {
                    Capsule.throwCapsule(heldItem, sendingPlayer, message.pos);
                }
            }
        });

        return null;

    }
}