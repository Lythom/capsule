package capsule.network.handler;

import capsule.Config;
import capsule.StructureSaver;
import capsule.helpers.Capsule;
import capsule.helpers.Spacial;
import capsule.items.CapsuleItem;
import capsule.network.CapsuleContentPreviewAnswerToClient;
import capsule.network.CapsuleContentPreviewQueryToServer;
import capsule.network.CapsuleFullContentAnswerToClient;
import capsule.network.CapsuleLeftClickQueryToServer;
import capsule.network.CapsuleThrowQueryToServer;
import capsule.network.LabelEditedMessageToServer;
import capsule.platform.Services;
import capsule.structure.CapsuleTemplate;
import capsule.structure.CapsuleTemplateManager;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.phys.AABB;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

import static capsule.items.CapsuleItem.CapsuleState.DEPLOYED;

public class ServerPayloadHandler {

	public static final ServerPayloadHandler INSTANCE = new ServerPayloadHandler();

	public static ServerPayloadHandler getInstance() {
		return INSTANCE;
	}

	public static void handleLabel(final LabelEditedMessageToServer data, final ServerPlayer sendingPlayer) {
		ItemStack serverStack = sendingPlayer.getMainHandItem();
		if (serverStack.getItem() instanceof CapsuleItem) {
			// of the player didn't swap item during ui opening
			CapsuleItem.setLabel(serverStack, data.label());
		}
	}

	public static void handleContentPreviewQuery(final CapsuleContentPreviewQueryToServer data, final ServerPlayer sendingPlayer) {
		// read the content of the template and send it back to the client
		ItemStack capsule = previewedCapsule(sendingPlayer, data.structureName());
		if (capsule == null) {
			return;
		}

		ServerLevel serverworld = (ServerLevel) sendingPlayer.level();
		Pair<CapsuleTemplateManager, CapsuleTemplate> templatepair = StructureSaver.getTemplate(capsule, serverworld);
		CapsuleTemplate template = templatepair.getRight();

		if (template != null) {
			List<AABB> blockspos = Spacial.mergeVoxels(template.getPalette());
			Services.NETWORK.sendToPlayer(sendingPlayer, new CapsuleContentPreviewAnswerToClient(blockspos, data.structureName()));
			Services.NETWORK.sendToPlayer(sendingPlayer, new CapsuleFullContentAnswerToClient(template, data.structureName()));
		} else {
			sendingPlayer.sendSystemMessage(Component.translatable("capsule.error.templateNotFound", data.structureName()));
		}
	}

	/**
	 * A capsule of the player using the asked template. Not just the held item: the client asks the preview of the item
	 * it takes in hand before telling the server about the new selected slot.
	 */
	@Nullable
	public static ItemStack previewedCapsule(Player player, String structureName) {
		return player.getInventory().items.stream()
				.filter(stack -> stack.getItem() instanceof CapsuleItem && structureName.equals(CapsuleItem.getStructureName(stack)))
				.findFirst()
				.orElse(null);
	}

	public static void handleThrowQuery(final CapsuleThrowQueryToServer data, final ServerPlayer sendingPlayer) {
		Capsule.handleThrowQuery(sendingPlayer, data.pos(), data.instant());
	}

	public static void handleLeftClickQuery(final CapsuleLeftClickQueryToServer data, final ServerPlayer sendingPlayer) {
		// read the content of the template and send it back to the client
		ItemStack stack = sendingPlayer.getMainHandItem();
		if (stack.getItem() instanceof CapsuleItem && CapsuleItem.isBlueprint(stack) && CapsuleItem.hasState(stack, DEPLOYED)) {
			// Reload if no missing materials
			ServerLevel serverLevel = (ServerLevel) sendingPlayer.level();
			Map<StructureSaver.ItemStackKey, Integer> missing = Capsule.reloadBlueprint(stack, serverLevel, sendingPlayer);
			if (missing != null && missing.size() > 0) {
				MutableComponent message = Component.literal("Missing :");
				for (Map.Entry<StructureSaver.ItemStackKey, Integer> entry : missing.entrySet()) {
					message.append("\n* " + entry.getValue() + " ");
					message.append(entry.getKey().itemStack.getItem().getName(entry.getKey().itemStack));
				}
				sendingPlayer.sendSystemMessage(message);
			}
		} else if (stack.getItem() instanceof CapsuleItem && CapsuleItem.canRotate(stack)) {
			StructurePlaceSettings placement = CapsuleItem.getPlacement(stack);
			if (sendingPlayer.isShiftKeyDown()) {
				if (Config.allowMirror) {
					switch (placement.getMirror()) {
						case FRONT_BACK:
							placement.setMirror(Mirror.LEFT_RIGHT);
							break;
						case LEFT_RIGHT:
							placement.setMirror(Mirror.NONE);
							break;
						case NONE:
							placement.setMirror(Mirror.FRONT_BACK);
							break;
					}
					sendingPlayer.sendSystemMessage(Component.translatable("[ ]: " + Capsule.getMirrorLabel(placement)));
				} else {
					sendingPlayer.sendSystemMessage(Component.translatable("Mirroring disabled by config"));
				}
			} else {
				placement.setRotation(placement.getRotation().getRotated(Rotation.CLOCKWISE_90));
				sendingPlayer.sendSystemMessage(Component.translatable("⟳: " + Capsule.getRotationLabel(placement)));
			}
			CapsuleItem.setPlacement(stack, placement);
		}
	}
}
