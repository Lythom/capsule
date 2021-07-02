package capsule.network;

import capsule.StructureSaver;
import capsule.structure.CapsuleTemplate;
import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * This Network Message is sent from the client to the server
 */
public class CapsuleFullContentAnswerToClient implements IMessage {

    protected static final Logger LOGGER = LogManager.getLogger(CapsuleFullContentAnswerToClient.class);
    public static final long BUFFER_MAX_SIZE = 1000000L;

    private CapsuleTemplate template = null;
    private String structureName = null;

    public CapsuleFullContentAnswerToClient(CapsuleTemplate template, String structureName) {
        this.template = template;
        this.structureName = structureName;
    }

    // for use by the message handler only.
    public CapsuleFullContentAnswerToClient() {

    }

    /**
     * Called by the network code once it has received the message bytes over
     * the network. Used to read the ByteBuf contents into your member variables
     *
     * @param buf buffer content to read from
     */
    @Override
    public void fromBytes(ByteBuf buf) {
        try {
            this.structureName = ByteBufUtils.readUTF8String(buf);
            boolean isSmallEnough = buf.readBoolean();
            this.template = null;
            if (isSmallEnough) {
                byte[] abyte = new byte[buf.readableBytes()];
                buf.readBytes(abyte);
                ByteArrayInputStream bytearrayoutputstream = new ByteArrayInputStream(abyte);
                NBTTagCompound nbt = null;
                try {
                    nbt = CompressedStreamTools.readCompressed(bytearrayoutputstream);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if (nbt != null) {
                    this.template = new CapsuleTemplate();
                    this.template.read(nbt);
                }
            }
        } catch (IndexOutOfBoundsException ioe) {
            LOGGER.error("Exception while reading CapsuleFullContentAnswerToClient: " + ioe);
        }
    }

    /**
     * Called by the network code. Used to write the contents of your message
     * member variables into the ByteBuf, ready for transmission over the
     * network.
     *
     * @param buf buffer content to write into
     */
    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufUtils.writeUTF8String(buf, this.structureName);
        NBTTagCompound nbt = StructureSaver.getTemplateNBTData(template);
        ByteArrayOutputStream bytearrayoutputstream = new ByteArrayOutputStream();
        try {
            CompressedStreamTools.writeCompressed(nbt, bytearrayoutputstream);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (bytearrayoutputstream.size() < BUFFER_MAX_SIZE) {
                buf.writeBoolean(true);
                buf.writeBytes(bytearrayoutputstream.toByteArray());
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