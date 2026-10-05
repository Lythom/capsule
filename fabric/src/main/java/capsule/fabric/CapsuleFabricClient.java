package capsule.fabric;

import capsule.CapsuleMod;
import capsule.blocks.CapsuleBlocks;
import capsule.blocks.CaptureBER;
import capsule.client.CapsulePreviewHandler;
import capsule.gui.LabelGui;
import capsule.items.CapsuleItem;
import capsule.items.CapsuleItems;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;

public class CapsuleFabricClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        CapsuleMod.openGuiScreenCommon = LabelGui::open;
        FabricPayloadRegistrar.registerClientReceivers();

        // unclamped, as on NeoForge: capsule states go above 1
        ItemProperties.PROPERTIES.computeIfAbsent(CapsuleItems.CAPSULE.get(), item -> new HashMap<>()).put(
                ResourceLocation.fromNamespaceAndPath(CapsuleMod.MODID, "state"),
                (stack, world, entity, seed) -> CapsuleItem.getState(stack).getValue()
        );
        ColorProviderRegistry.ITEM.register(CapsuleItem::getColorFromItemstack, CapsuleItems.CAPSULE.get());
        BlockEntityRenderers.register(CapsuleBlocks.MARKER_TE.get(), CaptureBER::new);

        WorldRenderEvents.AFTER_TRANSLUCENT.register(context -> CapsulePreviewHandler.onLevelRenderLast(
                context.worldRenderer(), context.matrixStack(), context.tickCounter().getGameTimeDeltaPartialTick(true)));
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> CapsulePreviewHandler.onChatMessage(message));
        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            if (client.player != null) CapsulePreviewHandler.onLocalPlayerTick(client.player);
        });
    }
}
