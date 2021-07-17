package capsule.network;

import capsule.client.CapsulePreviewHandler;
import capsule.helpers.Capsule;
import net.minecraft.client.Minecraft;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.StringUtils;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.network.NetworkEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.function.Supplier;

public class CapsuleUndeployNotifToClient {

    protected static final Logger LOGGER = LogManager.getLogger(CapsuleUndeployNotifToClient.class);

    public String templateName = null;
    public BlockPos posFrom = null;
    public BlockPos posTo = null;
    public int size = 0;

    public CapsuleUndeployNotifToClient(BlockPos posFrom, BlockPos posTo, int size, String templateName) {
        this.posFrom = posFrom;
        this.posTo = posTo;
        this.size = size;
        this.templateName = templateName;
    }

    public void onClient(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            Capsule.showUndeployParticules(Minecraft.getInstance().level, posFrom, posTo, size);
            if (!StringUtils.isNullOrEmpty(templateName)) {
                CapsulePreviewHandler.currentPreview.remove(templateName);
                CapsulePreviewHandler.currentFullPreview.remove(templateName);
            }
        });
        ctx.get().setPacketHandled(true);
    }

    public CapsuleUndeployNotifToClient(PacketBuffer buf) {
        try {
            this.posFrom = buf.readBlockPos();
            this.posTo = buf.readBlockPos();
            this.size = buf.readShort();
            this.templateName = buf.readUtf();
        } catch (IndexOutOfBoundsException ioe) {
            LOGGER.error("Exception while reading CapsuleUndeployNotifToClient: " + ioe);
        }
    }

    public void toBytes(PacketBuffer buf) {
        buf.writeBlockPos(posFrom);
        buf.writeBlockPos(posTo);
        buf.writeShort(size);
        buf.writeUtf(templateName == null ? "" : templateName);
    }

    @Override
    public String toString() {
        return getClass().toString();
    }

}