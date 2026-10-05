package capsule.network;

import capsule.CapsuleMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record CapsuleContentPreviewQueryToServer(String structureName) implements CustomPacketPayload {
	public CapsuleContentPreviewQueryToServer {
		if (structureName == null) structureName = "";
	}

	public static final CustomPacketPayload.Type<CapsuleContentPreviewQueryToServer> TYPE =
			new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(CapsuleMod.MODID, "content_preview_query"));

	public static final StreamCodec<ByteBuf, CapsuleContentPreviewQueryToServer> STREAM_CODEC =
			ByteBufCodecs.STRING_UTF8.map(CapsuleContentPreviewQueryToServer::new, CapsuleContentPreviewQueryToServer::structureName);

	@Override
	public CustomPacketPayload.Type<CapsuleContentPreviewQueryToServer> type() {
		return TYPE;
	}

	@Override
	public String toString() {
		return getClass().toString();
	}
}
