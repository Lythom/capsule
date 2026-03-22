package capsule.network;

import capsule.CapsuleMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record CapsuleLeftClickQueryToServer() implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<CapsuleLeftClickQueryToServer> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(CapsuleMod.MODID, "left_click_query"));

    public static final StreamCodec<ByteBuf, CapsuleLeftClickQueryToServer> STREAM_CODEC =
            StreamCodec.of(
                    (buf, msg) -> {},
                    buf -> new CapsuleLeftClickQueryToServer()
            );

    @Override
    public CustomPacketPayload.Type<CapsuleLeftClickQueryToServer> type() {
        return TYPE;
    }

    @Override
    public String toString() {
        return getClass().toString();
    }

}
