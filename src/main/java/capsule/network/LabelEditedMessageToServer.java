package capsule.network;

import capsule.items.CapsuleItem;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.function.Supplier;

/**
 * This Network Message is sent from the client to the server
 */
public class LabelEditedMessageToServer {

    protected static final Logger LOGGER = LogManager.getLogger(LabelEditedMessageToServer.class);

    private String label;

    public LabelEditedMessageToServer(String newLabel) {
        setLabel(newLabel);
    }

    public LabelEditedMessageToServer(PacketBuffer buf) {
        try {
            this.setLabel(buf.readString(32767));
        } catch (IndexOutOfBoundsException ioe) {
            LOGGER.error("Exception while reading CapsuleLabelEditedMessageToClient: " + ioe);
        }
    }

    public void toBytes(PacketBuffer buf) {
        buf.writeString(this.label);
    }

    public void onServer(Supplier<NetworkEvent.Context> ctx) {
        final ServerPlayerEntity sendingPlayer = ctx.get().getSender();
        if (sendingPlayer == null) {
            LOGGER.error("ServerPlayerEntity was null when LabelEditedMessageToServer was received");
            return;
        }
        ctx.get().enqueueWork(() -> {
            ItemStack serverStack = sendingPlayer.getHeldItemMainhand();
            if (serverStack.getItem() instanceof CapsuleItem) {
                // of the player didn't swap item during ui opening
                CapsuleItem.setLabel(serverStack, getLabel());
            }
        });
        ctx.get().setPacketHandled(true);
    }

    @Override
    public String toString() {
        return getClass().toString() + "[label=" + getLabel() + "]";
    }

    public String getLabel() {
        return label;
    }
    public void setLabel(String label) {
        this.label = label;
    }

}