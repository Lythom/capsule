package capsule.network;

import capsule.StructureSaver;
import capsule.structure.CapsuleTemplate;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.function.Supplier;

/**
 * This Network Message is sent from the client to the server
 */
public class CapsuleFullContentAnswerToClient {

    protected static final Logger LOGGER = LogManager.getLogger(CapsuleFullContentAnswerToClient.class);
    public static final long BUFFER_MAX_SIZE = 1000000L;

    private CapsuleTemplate template = null;
    private String structureName = null;

    public CapsuleFullContentAnswerToClient(CapsuleTemplate template, String structureName) {
        this.template = template;
        this.structureName = structureName;
    }

    public void onClient(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            if (template != null) {
                synchronized (capsule.client.CapsulePreviewHandler.currentFullPreview) {
                    capsule.client.CapsulePreviewHandler.currentFullPreview.put(getStructureName(), template);
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }

    public CapsuleFullContentAnswerToClient(PacketBuffer buf) {
        try {
            this.structureName = buf.readUtf(32767);
            boolean isSmallEnough = buf.readBoolean();
            this.template = null;
            if (isSmallEnough) {
                ByteArrayInputStream bytearrayoutputstream = new ByteArrayInputStream(buf.readByteArray());
                CompoundNBT nbt = null;
                try {
                    nbt = CompressedStreamTools.readCompressed(bytearrayoutputstream);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if (nbt != null) {
                    this.template = new CapsuleTemplate();
                    this.template.load(nbt, "`networked from server`");
                }
            }
        } catch (IndexOutOfBoundsException ioe) {
            LOGGER.error("Exception while reading CapsuleFullContentAnswerToClient: " + ioe);
        }
    }

    public void toBytes(PacketBuffer buf) {
        buf.writeUtf(this.structureName);
        CompoundNBT nbt = StructureSaver.getTemplateNBTData(template);
        ByteArrayOutputStream bytearrayoutputstream = new ByteArrayOutputStream();
        try {
            CompressedStreamTools.writeCompressed(nbt, bytearrayoutputstream);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (bytearrayoutputstream.size() < BUFFER_MAX_SIZE) {
                buf.writeBoolean(true);
                buf.writeByteArray(bytearrayoutputstream.toByteArray());
            } else {
                buf.writeBoolean(false);
            }
        }
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