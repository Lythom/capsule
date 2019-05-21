package capsule.network.server;

import capsule.StructureSaver;
import capsule.items.CapsuleItem;
import capsule.network.CapsuleContentPreviewAnswerToClient;
import capsule.network.CapsuleContentPreviewQueryToServer;
import capsule.structure.CapsuleTemplate;
import capsule.structure.CapsuleTemplateManager;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.WorldServer;
import net.minecraft.world.gen.structure.template.Template;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * The MessageHandlerOnServer is used to process the network message once it has
 * arrived on the Server side. WARNING! In 1.8 the MessageHandler now runs in
 * its own thread. This means that if your onMessage code calls any vanilla
 * objects, it may cause crashes or subtle problems that are hard to reproduce.
 * Your onMessage handler should create a task which is later executed by the
 * client or server thread as appropriate - see below. User: The Grey Ghost
 * Date: 15/01/2015
 */
public class CapsuleContentPreviewQueryHandler
        implements IMessageHandler<CapsuleContentPreviewQueryToServer, CapsuleContentPreviewAnswerToClient> {

    protected static final Logger LOGGER = LogManager.getLogger(CapsuleContentPreviewQueryHandler.class);

    /**
     * Called when a message is received of the appropriate type. CALLED BY THE
     * NETWORK THREAD
     *
     * @param message The message
     */
    public CapsuleContentPreviewAnswerToClient onMessage(final CapsuleContentPreviewQueryToServer message,
                                                         MessageContext ctx) {
        if (ctx.side != Side.SERVER) {
            LOGGER.error("AskCapsuleContentPreviewMessageToServer received on wrong side:" + ctx.side);
            return null;
        }
        if (!message.isMessageValid()) {
            LOGGER.error("AskCapsuleContentPreviewMessageToServer was invalid" + message.toString());
            return null;
        }

        // we know for sure that this handler is only used on the server side,
        // so it is ok to assume
        // that the ctx handler is a serverhandler, and that WorldServer exists.
        // Packets received on the client side must be handled differently! See
        // MessageHandlerOnClient
        final EntityPlayerMP sendingPlayer = ctx.getServerHandler().player;
        if (sendingPlayer == null) {
            LOGGER.error("EntityPlayerMP was null when AskCapsuleContentPreviewMessageToServer was received");
            return null;
        }

        // read the content of the template and send it back to the client
        ItemStack heldItem = sendingPlayer.getHeldItemMainhand();
        if (!(heldItem.getItem() instanceof CapsuleItem) || CapsuleItem.getStructureName(heldItem) == null) {
            return null;
        }

        WorldServer serverworld = sendingPlayer.getServerWorld();
        Pair<CapsuleTemplateManager, CapsuleTemplate> templatepair = StructureSaver.getTemplate(heldItem, serverworld);
        CapsuleTemplate template = templatepair.getRight();

        if (template != null) {
            List<Template.BlockInfo> blocksInfos = template.blocks;
            List<BlockPos> blockspos = new ArrayList<>();
            for (Template.BlockInfo blockInfo : blocksInfos) {
                if (blockInfo.blockState != Blocks.AIR.getDefaultState()) {
                    blockspos.add(blockInfo.pos);
                }
            }

            return new CapsuleContentPreviewAnswerToClient(blockspos, message.getStructureName());

        } else if (heldItem.hasTagCompound()){
            //noinspection ConstantConditions
            String structureName = heldItem.getTagCompound().getString("structureName");
            sendingPlayer.sendMessage(new TextComponentTranslation("capsule.error.templateNotFound", structureName));
        }

        return null;

    }
}