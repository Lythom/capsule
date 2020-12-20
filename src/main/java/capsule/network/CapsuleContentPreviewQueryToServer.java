package capsule.network;

import capsule.StructureSaver;
import capsule.helpers.Spacial;
import capsule.items.CapsuleItem;
import capsule.structure.CapsuleTemplate;
import capsule.structure.CapsuleTemplateManager;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.fml.network.NetworkEvent;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.function.Supplier;

public class CapsuleContentPreviewQueryToServer {

    protected static final Logger LOGGER = LogManager.getLogger(CapsuleContentPreviewQueryToServer.class);

    private String structureName = null;

    public CapsuleContentPreviewQueryToServer(String structureName) {
        this.setStructureName(structureName);
    }

    public CapsuleContentPreviewQueryToServer(PacketBuffer buf) {
        try {
            this.setStructureName(buf.readString(32767));

        } catch (IndexOutOfBoundsException ioe) {
            LOGGER.error("Exception while reading AskCapsuleContentPreviewMessageToServer: " + ioe);
        }
    }

    public void toBytes(PacketBuffer buf) {
        if (buf == null) return;
        String name = this.getStructureName();
        if (name == null) name = "";
        buf.writeString(name);
    }

    public void onServer(Supplier<NetworkEvent.Context> ctx) {
        final ServerPlayerEntity sendingPlayer = ctx.get().getSender();
        if (sendingPlayer == null) {
            LOGGER.error("ServerPlayerEntity was null when AskCapsuleContentPreviewMessageToServer was received");
            return;
        }

        ctx.get().enqueueWork(() -> {
            // read the content of the template and send it back to the client
            ItemStack heldItem = sendingPlayer.getHeldItemMainhand();
            if (!(heldItem.getItem() instanceof CapsuleItem) || CapsuleItem.getStructureName(heldItem) == null) {
                return;
            }

            ServerWorld serverworld = sendingPlayer.getServerWorld();
            Pair<CapsuleTemplateManager, CapsuleTemplate> templatepair = StructureSaver.getTemplate(heldItem, serverworld);
            CapsuleTemplate template = templatepair.getRight();

            if (template != null) {
                List<AxisAlignedBB> blockspos = Spacial.mergeVoxels(template.getBlocks());
                CapsuleNetwork.wrapper.reply(new CapsuleContentPreviewAnswerToClient(blockspos, this.getStructureName()), ctx.get());
                CapsuleNetwork.wrapper.reply(new CapsuleFullContentAnswerToClient(template, this.getStructureName()), ctx.get());
            } else if (heldItem.hasTag()) {
                //noinspection ConstantConditions
                String structureName = heldItem.getTag().getString("structureName");
                sendingPlayer.sendMessage(new TranslationTextComponent("capsule.error.templateNotFound", structureName));
            }
        });
        ctx.get().setPacketHandled(true);
    }

    @Override
    public String toString() {
        return getClass().toString();
    }

    public String getStructureName() {
        return structureName;
    }

    public void setStructureName(String structureName) {
        this.structureName = structureName;
    }

}