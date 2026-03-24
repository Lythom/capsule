package capsule.network.handler;

import capsule.client.CapsulePreviewHandler;
import capsule.client.render.CapsuleTemplateRenderer;
import capsule.helpers.Capsule;
import capsule.network.CapsuleContentPreviewAnswerToClient;
import capsule.network.CapsuleContentPreviewQueryToServer;
import capsule.network.CapsuleFullContentAnswerToClient;
import capsule.network.CapsuleUndeployNotifToClient;
import capsule.structure.CapsuleTemplate;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.util.StringUtil;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ClientPayloadHandler {
	private static final Logger LOGGER = LogManager.getLogger(ClientPayloadHandler.class);
	private static final ClientPayloadHandler INSTANCE = new ClientPayloadHandler();

	public static ClientPayloadHandler getInstance() {
		return INSTANCE;
	}

	public static void handleContentPreviewAnswer(final CapsuleContentPreviewAnswerToClient data, final IPayloadContext context) {
		context.enqueueWork(() -> {
					synchronized (CapsulePreviewHandler.currentPreview) {
						CapsulePreviewHandler.currentPreview.put(data.structureName(), data.boundingBoxes());
					}
				})
				.exceptionally(e -> {
					LOGGER.error("Error handling content preview answer packet", e);
					return null;
				});
	}

	public static void handleFullContentAnswer(final CapsuleFullContentAnswerToClient data, final IPayloadContext context) {
		context.enqueueWork(() -> {
					synchronized (CapsulePreviewHandler.currentFullPreview) {
						String structureName = data.structureName();
						CapsuleTemplate template = data.template();
						CapsulePreviewHandler.currentFullPreview.put(structureName, template);
						if (CapsulePreviewHandler.cachedFullPreview.containsKey(structureName)) {
							CapsulePreviewHandler.cachedFullPreview.get(structureName).setWorldDirty();
						} else {
							CapsulePreviewHandler.cachedFullPreview.put(structureName, new CapsuleTemplateRenderer());
						}
					}
				})
				.exceptionally(e -> {
					LOGGER.error("Error handling full content answer packet", e);
					return null;
				});
	}

	public static void handleUndeployNotif(final CapsuleUndeployNotifToClient data, final IPayloadContext context) {
		context.enqueueWork(() -> {
					BlockPos posFrom = data.posFrom();
					BlockPos posTo = data.posTo();
					int size = data.size();
					String templateName = data.templateName();
					Capsule.showUndeployParticules(Minecraft.getInstance().level, posFrom, posTo, size);
					if (!StringUtil.isNullOrEmpty(templateName)) {
						// remove templates because they are dirty and must be redownloaded
						CapsulePreviewHandler.currentPreview.remove(templateName);
						CapsulePreviewHandler.currentFullPreview.remove(templateName);
						// ask a preview refresh
						PacketDistributor.sendToServer(new CapsuleContentPreviewQueryToServer(templateName));
					}
				})
				.exceptionally(e -> {
					LOGGER.error("Error handling undeploy notification packet", e);
					return null;
				});
	}
}
