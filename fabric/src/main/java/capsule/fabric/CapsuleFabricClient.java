package capsule.fabric;

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
import fuzs.forgeconfigapiport.fabric.api.v5.ConfigRegistry;
import fuzs.forgeconfigapiport.fabric.api.v5.ModConfigEvents;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.recipe.v1.sync.ClientRecipeSynchronizedEvent;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelExtractionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.client.color.item.ItemTintSources;
import net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperties;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

public class CapsuleFabricClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ModConfigSpec config = ModConfigSpecBuilder.build(ClientConfig::define);
        ModConfigEvents.loading(CapsuleMod.MODID).register(modConfig -> {
            if (modConfig.getSpec() == config) ClientConfig.bakeConfig();
        });
        ModConfigEvents.reloading(CapsuleMod.MODID).register(modConfig -> {
            if (modConfig.getSpec() == config) ClientConfig.bakeConfig();
        });
        ConfigRegistry.INSTANCE.register(CapsuleMod.MODID, ModConfig.Type.CLIENT, config);

        CapsuleMod.openGuiScreenCommon = LabelGui::open;
        FabricPayloadRegistrar.registerClientReceivers();

        // read by assets/capsule/items/capsule.json; both id mappers are opened by Fabric API's transitive class tweaker
        RangeSelectItemModelProperties.ID_MAPPER.put(CapsuleStateProperty.ID, CapsuleStateProperty.MAP_CODEC);
        ItemTintSources.ID_MAPPER.put(CapsuleTintSource.ID, CapsuleTintSource.MAP_CODEC);
        // compiled with the vanilla pipelines at resource reload (RenderPipelines#register is opened by Fabric API)
        CustomRenderType.PIPELINES.forEach(RenderPipelines::register);
        BlockEntityRenderers.register(CapsuleBlocks.MARKER_TE.get(), CaptureBER::new);

        // gathered during the level extraction, submitted to the frame's collector (not drawn after the translucent
        // terrain: 26.3 skips that stage when Improved Transparency is on)
        LevelExtractionEvents.END_EXTRACTION.register(context -> CapsulePreviewHandler.extractFrame(
                context.camera().position(), context.deltaTracker().getGameTimeDeltaPartialTick(true)));
        LevelRenderEvents.COLLECT_SUBMITS.register(context -> CapsulePreviewHandler.submitFrame(
                context.submitNodeCollector(), context.poseStack()));
        ClientRecipeSynchronizedEvent.EVENT.register((client, recipes) -> CapsuleItems.registerRecipesClient(recipes.getAllOfType(RecipeType.CRAFTING)));
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> CapsulePreviewHandler.onChatMessage(message));
        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            if (client.player != null) CapsulePreviewHandler.onLocalPlayerTick(client.player);
        });
    }
}
