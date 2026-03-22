package capsule.network;

import capsule.CapsuleMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * This Network Message is sent from the client to the server
 */
public record LabelEditedMessageToServer(String label) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<LabelEditedMessageToServer> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(CapsuleMod.MODID, "label_edited_message"));

    public static final StreamCodec<ByteBuf, LabelEditedMessageToServer> STREAM_CODEC =
            ByteBufCodecs.STRING_UTF8.map(LabelEditedMessageToServer::new, LabelEditedMessageToServer::label);

    @Override
    public CustomPacketPayload.Type<LabelEditedMessageToServer> type() {
        return TYPE;
    }

    @Override
    public String toString() {
        return getClass().toString() + "[label=" + label() + "]";
    }

}
