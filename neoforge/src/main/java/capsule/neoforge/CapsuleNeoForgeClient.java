package capsule.neoforge;

import capsule.CapsuleMod;
import capsule.blocks.CapsuleBlocks;
import capsule.blocks.CaptureBER;
import capsule.client.CapsulePreviewHandler;
import capsule.client.ClientConfig;
import capsule.client.render.CapsuleStateProperty;
import capsule.client.render.CapsuleTintSource;
import capsule.client.render.CustomRenderType;
import capsule.gui.LabelGui;
import capsule.items.CapsuleItem;
import capsule.items.CapsuleItems;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.client.event.ClientChatReceivedEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ExtractLevelRenderStateEvent;
import net.neoforged.neoforge.client.event.RecipesReceivedEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterRangeSelectItemModelPropertyEvent;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;
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

        // read by assets/capsule/items/capsule.json
        modEventBus.addListener((RegisterRangeSelectItemModelPropertyEvent event) ->
                event.register(CapsuleStateProperty.ID, CapsuleStateProperty.MAP_CODEC));
        modEventBus.addListener((RegisterColorHandlersEvent.ItemTintSources event) ->
                event.register(CapsuleTintSource.ID, CapsuleTintSource.MAP_CODEC));
        modEventBus.addListener((RegisterRenderPipelinesEvent event) -> CustomRenderType.PIPELINES.forEach(event::registerPipeline));
        modEventBus.addListener((EntityRenderersEvent.RegisterRenderers event) ->
                event.registerBlockEntityRenderer(CapsuleBlocks.MARKER_TE.get(), CaptureBER::new));

        IEventBus bus = NeoForge.EVENT_BUS;
        bus.addListener((RecipesReceivedEvent event) -> CapsuleItems.registerRecipesClient(event.getRecipeMap().byType(RecipeType.CRAFTING)));
        // gathered during the level extraction, submitted to the frame's collector (not drawn after the translucent
        // blocks: 26.3 skips that stage when Improved Transparency is on)
        bus.addListener((ExtractLevelRenderStateEvent event) -> CapsulePreviewHandler.extractFrame(
                event.getCamera().position(), event.getDeltaTracker().getGameTimeDeltaPartialTick(true)));
        bus.addListener((SubmitCustomGeometryEvent event) -> CapsulePreviewHandler.submitFrame(
                event.getSubmitNodeCollector(), event.getPoseStack()));
        bus.addListener((ClientChatReceivedEvent event) -> CapsulePreviewHandler.onChatMessage(event.getMessage()));
        bus.addListener((PlayerTickEvent.Pre event) -> {
            if (event.getEntity() instanceof LocalPlayer player) CapsulePreviewHandler.onLocalPlayerTick(player);
        });
        bus.addListener((PlayerInteractEvent.LeftClickEmpty event) -> CapsuleItem.onLeftClickEmpty(event.getEntity()));
    }
}
