package capsule.network;

import capsule.CapsuleMod;
import capsule.StructureSaver;
import capsule.structure.CapsuleTemplate;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * This Network Message is sent from the server to the client
 */
public record CapsuleFullContentAnswerToClient(String structureName, CapsuleTemplate template) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<CapsuleFullContentAnswerToClient> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(CapsuleMod.MODID, "full_content_answer"));

    protected static final Logger LOGGER = LogManager.getLogger(CapsuleFullContentAnswerToClient.class);
    public static final long BUFFER_MAX_SIZE = 1000000L;

    public static final StreamCodec<FriendlyByteBuf, CapsuleFullContentAnswerToClient> STREAM_CODEC =
            StreamCodec.of(CapsuleFullContentAnswerToClient::encode, CapsuleFullContentAnswerToClient::decode);

    public CapsuleFullContentAnswerToClient(CapsuleTemplate template, String structureName) {
        this(structureName, template);
    }

    private static CapsuleFullContentAnswerToClient decode(FriendlyByteBuf buf) {
        String structureName = buf.readUtf();
        boolean isSmallEnough = buf.readBoolean();
        CapsuleTemplate template = null;
        if (isSmallEnough) {
            ByteArrayInputStream bytearrayoutputstream = new ByteArrayInputStream(buf.readByteArray());
            CompoundTag nbt = null;
            try {
                nbt = NbtIo.readCompressed(bytearrayoutputstream, NbtAccounter.unlimitedHeap());
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (nbt != null) {
                template = new CapsuleTemplate();
                template.load(nbt, "`networked from server`");
            }
        }
        return new CapsuleFullContentAnswerToClient(structureName, template);
    }

    private static void encode(FriendlyByteBuf buf, CapsuleFullContentAnswerToClient msg) {
        buf.writeUtf(msg.structureName);
        CompoundTag nbt = StructureSaver.getTemplateNBTData(msg.template);
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
    public CustomPacketPayload.Type<CapsuleFullContentAnswerToClient> type() {
        return TYPE;
    }

    @Override
    public String toString() {
        return getClass().toString();
    }
}
