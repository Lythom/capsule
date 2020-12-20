package capsule.network;

import capsule.StructureSaver;
import capsule.structure.CapsuleTemplate;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.function.Supplier;

/**
 * This Network Message is sent from the client to the server
 */
public class CapsuleFullContentAnswerToClient {

    protected static final Logger LOGGER = LogManager.getLogger(CapsuleFullContentAnswerToClient.class);

    private CapsuleTemplate template = null;
    private String structureName = null;

    public CapsuleFullContentAnswerToClient(CapsuleTemplate template, String structureName) {
        this.template = template;
        this.structureName = structureName;
    }

    public void onClient(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            synchronized (capsule.client.CapsulePreviewHandler.currentFullPreview) {
                capsule.client.CapsulePreviewHandler.currentFullPreview.put(getStructureName(), template);
            }
        });
        ctx.get().setPacketHandled(true);
    }

    public CapsuleFullContentAnswerToClient(PacketBuffer buf) {
        try {
            this.structureName = buf.readString(32767);
            this.template = new CapsuleTemplate();
            CompoundNBT nbt = buf.readCompoundTag();
            if (nbt != null) this.template.read(nbt);
        } catch (IndexOutOfBoundsException ioe) {
            LOGGER.error("Exception while reading CapsuleFullContentAnswerToClient: " + ioe);
        }
    }

    public void toBytes(PacketBuffer buf) {
        buf.writeString(this.structureName);
        buf.writeCompoundTag(StructureSaver.getTemplateNBTData(template));
    }

    @Override
    public String toString() {
        return getClass().toString();
    }

    public String getStructureName() {
        return structureName;
    }

    public CapsuleTemplate getTemplate() {
        return template;
    }

}