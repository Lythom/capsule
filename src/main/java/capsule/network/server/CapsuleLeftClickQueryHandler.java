package capsule.network.server;

import capsule.StructureSaver;
import capsule.helpers.Capsule;
import capsule.items.CapsuleItem;
import capsule.network.CapsuleLeftClickQueryToServer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.gen.structure.template.PlacementSettings;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;
import java.util.stream.Collectors;

import static capsule.items.CapsuleItem.*;

/**
 * The MessageHandlerOnServer is used to process the network message once it has
 * arrived on the Server side. WARNING! In 1.8 the MessageHandler now runs in
 * its own thread. This means that if your onMessage code calls any vanilla
 * objects, it may cause crashes or subtle problems that are hard to reproduce.
 * Your onMessage handler should create a task which is later executed by the
 * client or server thread as appropriate - see below. User: The Grey Ghost
 * Date: 15/01/2015
 */
public class CapsuleLeftClickQueryHandler
        implements IMessageHandler<CapsuleLeftClickQueryToServer, IMessage> {

    protected static final Logger LOGGER = LogManager.getLogger(CapsuleLeftClickQueryHandler.class);

    /**
     * Called when a message is received of the appropriate type. CALLED BY THE
     * NETWORK THREAD
     *
     * @param message The message
     */
    public IMessage onMessage(final CapsuleLeftClickQueryToServer message,
                              MessageContext ctx) {
        if (ctx.side != Side.SERVER) {
            LOGGER.error("CapsuleLeftClickQueryToServer received on wrong side:" + ctx.side);
            return null;
        }
        if (!message.isMessageValid()) {
            LOGGER.error("CapsuleLeftClickQueryToServer was invalid" + message.toString());
            return null;
        }

        // we know for sure that this handler is only used on the server side,
        // so it is ok to assume
        // that the ctx handler is a serverhandler, and that WorldServer exists.
        // Packets received on the client side must be handled differently! See
        // MessageHandlerOnClient
        final EntityPlayerMP sendingPlayer = ctx.getServerHandler().player;
        if (sendingPlayer == null) {
            LOGGER.error("EntityPlayerMP was null when CapsuleLeftClickQueryToServer was received");
            return null;
        }

        // Execute the action on the main server thread by adding it as a scheduled task
        sendingPlayer.getServerWorld().addScheduledTask(() -> {
            // read the content of the template and send it back to the client
            ItemStack stack = sendingPlayer.getHeldItemMainhand();
            if (stack.getItem() instanceof CapsuleItem && CapsuleItem.isBlueprint(stack)) {
                if (stack.getItemDamage() == STATE_DEPLOYED) {
                    // Reload if no missing materials
                    Map<StructureSaver.ItemStackKey, Integer> missing = Capsule.reloadBlueprint(stack, sendingPlayer.getServerWorld(), sendingPlayer);
                    if (missing.size() > 0) {
                        String missingListText = missing.entrySet().stream().map((entry) -> (entry.getValue() + " " + entry.getKey().itemStack.getItem().getItemStackDisplayName(entry.getKey().itemStack))).collect(Collectors.joining("\n* "));
                        sendingPlayer.sendMessage(new TextComponentTranslation(
                                "Missing : \n* " + missingListText
                        ));
                    }
                } else if (stack.getItemDamage() == STATE_BLUEPRINT) {
                    PlacementSettings placement = getPlacement(stack);
                    if (sendingPlayer.isSneaking()) {
                        switch (placement.getMirror()) {
                            case FRONT_BACK:
                                placement.setMirror(Mirror.LEFT_RIGHT);
                                break;
                            case LEFT_RIGHT:
                                placement.setMirror(Mirror.NONE);
                                break;
                            case NONE:
                                placement.setMirror(Mirror.FRONT_BACK);
                                break;
                        }
                        sendingPlayer.sendMessage(new TextComponentTranslation("[ ]: " + Capsule.getMirrorLabel(placement)));
                    } else {
                        placement.setRotation(placement.getRotation().add(Rotation.CLOCKWISE_90));
                        sendingPlayer.sendMessage(new TextComponentTranslation("⟳: " + Capsule.getRotationLabel(placement)));
                    }
                    setPlacement(stack, placement);
                }
            }

        });

        return null;
    }
}