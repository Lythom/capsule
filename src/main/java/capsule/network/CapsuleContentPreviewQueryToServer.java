package capsule.network;

import capsule.StructureSaver;
import capsule.helpers.Spacial;
import capsule.items.CapsuleItem;
import capsule.structure.CapsuleTemplate;
import capsule.structure.CapsuleTemplateManager;
import net.minecraft.Util;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.network.NetworkEvent;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Supplier;

public class CapsuleContentPreviewQueryToServer {

    protected static final Logger LOGGER = LogManager.getLogger(CapsuleContentPreviewQueryToServer.class);

    private String structureName = null;

    public CapsuleContentPreviewQueryToServer(String structureName) {
        this.setStructureName(structureName);
    }

    public CapsuleContentPreviewQueryToServer(FriendlyByteBuf buf) {
        try {
            this.setStructureName(buf.readUtf(32767));

        } catch (IndexOutOfBoundsException ioe) {
            LOGGER.error("Exception while reading AskCapsuleContentPreviewMessageToServer: " + ioe);
        }
    }

    public void toBytes(FriendlyByteBuf buf) {
        if (buf == null) return;
        String name = this.getStructureName();
        if (name == null) name = "";
        buf.writeUtf(name);
    }

    /**
     * A capsule of the player using the asked template. Not just the held item: the client asks the preview of the item
     * it takes in hand before telling the server about the new selected slot.
     */
    @Nullable
    public static ItemStack previewedCapsule(Player player, String structureName) {
        return player.getInventory().items.stream()
                .filter(stack -> stack.getItem() instanceof CapsuleItem && structureName.equals(CapsuleItem.getStructureName(stack)))
                .findFirst()
                .orElse(null);
    }

    public void onServer(Supplier<NetworkEvent.Context> ctx) {
        final ServerPlayer sendingPlayer = ctx.get().getSender();
        if (sendingPlayer == null) {
            LOGGER.error("ServerPlayerEntity was null when AskCapsuleContentPreviewMessageToServer was received");
            return;
        }

        ctx.get().enqueueWork(() -> {
            // read the content of the template and send it back to the client
            ItemStack capsule = previewedCapsule(sendingPlayer, this.getStructureName());
            if (capsule == null) {
                return;
            }

            ServerLevel serverworld = sendingPlayer.getLevel();
            Pair<CapsuleTemplateManager, CapsuleTemplate> templatepair = StructureSaver.getTemplate(capsule, serverworld);
            CapsuleTemplate template = templatepair.getRight();

            if (template != null) {
                List<AABB> blockspos = Spacial.mergeVoxels(template.getPalette());
                CapsuleNetwork.wrapper.reply(new CapsuleContentPreviewAnswerToClient(blockspos, this.getStructureName()), ctx.get());
                CapsuleNetwork.wrapper.reply(new CapsuleFullContentAnswerToClient(template, this.getStructureName()), ctx.get());
            } else {
                sendingPlayer.sendMessage(new TranslatableComponent("capsule.error.templateNotFound", this.getStructureName()), Util.NIL_UUID);
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