package capsule.network.handler;

import capsule.client.CaptureAnimation;
import capsule.client.CapsulePreviewHandler;
import capsule.client.render.CapsuleTemplateRenderer;
import capsule.helpers.Capsule;
import capsule.network.CapsuleContentPreviewAnswerToClient;
import capsule.network.CapsuleContentPreviewQueryToServer;
import capsule.network.CapsuleFullContentAnswerToClient;
import capsule.network.CapsuleUndeployNotifToClient;
import capsule.platform.Services;
import capsule.structure.CapsuleTemplate;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.util.StringUtil;

public class ClientPayloadHandler {
	private static final ClientPayloadHandler INSTANCE = new ClientPayloadHandler();

	public static ClientPayloadHandler getInstance() {
		return INSTANCE;
	}

	public static void handleContentPreviewAnswer(final CapsuleContentPreviewAnswerToClient data) {
		synchronized (CapsulePreviewHandler.currentPreview) {
			CapsulePreviewHandler.currentPreview.put(data.structureName(), data.boundingBoxes());
		}
	}

	public static void handleFullContentAnswer(final CapsuleFullContentAnswerToClient data) {
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
	}

	public static void handleUndeployNotif(final CapsuleUndeployNotifToClient data) {
		BlockPos posFrom = data.posFrom();
		BlockPos posTo = data.posTo();
		int size = data.size();
		String templateName = data.templateName();
		Capsule.showUndeployParticules(Minecraft.getInstance().level, posFrom, posTo, size);
		CaptureAnimation.start(Minecraft.getInstance().level, posFrom, posTo, size);
		if (!StringUtil.isNullOrEmpty(templateName)) {
			// remove templates because they are dirty and must be redownloaded
			CapsulePreviewHandler.currentPreview.remove(templateName);
			CapsulePreviewHandler.currentFullPreview.remove(templateName);
			// ask a preview refresh
			Services.NETWORK.sendToServer(new CapsuleContentPreviewQueryToServer(templateName));
		}
	}
}
