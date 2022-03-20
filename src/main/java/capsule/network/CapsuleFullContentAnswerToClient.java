package capsule.network;

import capsule.StructureSaver;
import capsule.client.render.CapsuleTemplateRenderer;
import capsule.structure.CapsuleTemplate;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.FriendlyByteBuf;
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
                    if (capsule.client.CapsulePreviewHandler.cachedFullPreview.containsKey(getStructureName())) {
                        capsule.client.CapsulePreviewHandler.cachedFullPreview.get(getStructureName()).setWorldDirty();
                    } else {
                        capsule.client.CapsulePreviewHandler.cachedFullPreview.put(getStructureName(), new CapsuleTemplateRenderer());
                    }
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }

    public CapsuleFullContentAnswerToClient(FriendlyByteBuf buf) {
        try {
            this.structureName = buf.readUtf(32767);
            boolean isSmallEnough = buf.readBoolean();
            this.template = null;
            if (isSmallEnough) {
                ByteArrayInputStream bytearrayoutputstream = new ByteArrayInputStream(buf.readByteArray());
                CompoundTag nbt = null;
                try {
                    nbt = NbtIo.readCompressed(bytearrayoutputstream);
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

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUtf(this.structureName);
        CompoundTag nbt = StructureSaver.getTemplateNBTData(template);
        ByteArrayOutputStream bytearrayoutputstream = new ByteArrayOutputStream();
        try {
            NbtIo.writeCompressed(nbt, bytearrayoutputstream);
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