package capsule.neoforge;

import capsule.CapsuleMod;
import capsule.blocks.CapsuleBlocks;
import capsule.blocks.CaptureBER;
import capsule.client.CapsulePreviewHandler;
import capsule.client.ClientConfig;
import capsule.gui.LabelGui;
import capsule.items.CapsuleItem;
import capsule.items.CapsuleItems;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientChatReceivedEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RecipesUpdatedEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

public class CapsuleNeoForgeClient {

    static void init(IEventBus modEventBus, ModContainer modContainer) {
        ModConfigSpec config = ModConfigSpecBuilder.build(ClientConfig::define);
        modContainer.registerConfig(ModConfig.Type.CLIENT, config);
        modEventBus.addListener((ModConfigEvent event) -> {
            if (event.getConfig().getSpec() == config) ClientConfig.bakeConfig();
        });

        CapsuleMod.openGuiScreenCommon = LabelGui::open;

        modEventBus.addListener((FMLClientSetupEvent event) -> event.enqueueWork(() -> ItemProperties.register(
                CapsuleItems.CAPSULE.get(),
                ResourceLocation.fromNamespaceAndPath(CapsuleMod.MODID, "state"),
                (stack, world, entity, seed) -> CapsuleItem.getState(stack).getValue()
        )));
        modEventBus.addListener((RegisterColorHandlersEvent.Item event) ->
                event.register(CapsuleItem::getColorFromItemstack, CapsuleItems.CAPSULE.get()));
        modEventBus.addListener((EntityRenderersEvent.RegisterRenderers event) ->
                event.registerBlockEntityRenderer(CapsuleBlocks.MARKER_TE.get(), CaptureBER::new));

        IEventBus bus = NeoForge.EVENT_BUS;
        bus.addListener((RecipesUpdatedEvent event) -> CapsuleItems.registerRecipesClient(event.getRecipeManager()));
        bus.addListener((RenderLevelStageEvent event) -> {
            if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
                CapsulePreviewHandler.onLevelRenderLast(event.getLevelRenderer(), event.getPoseStack(), event.getPartialTick().getGameTimeDeltaPartialTick(true));
            }
        });
        bus.addListener((ClientChatReceivedEvent event) -> CapsulePreviewHandler.onChatMessage(event.getMessage()));
        bus.addListener((PlayerTickEvent.Pre event) -> {
            if (event.getEntity() instanceof LocalPlayer player) CapsulePreviewHandler.onLocalPlayerTick(player);
        });
        bus.addListener((PlayerInteractEvent.LeftClickEmpty event) -> CapsuleItem.onLeftClickEmpty(event.getEntity()));
    }
}
