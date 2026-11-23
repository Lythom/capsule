package capsule.fabric;

import capsule.CapsuleMod;
import capsule.blocks.CapsuleBlocks;
import capsule.blocks.CaptureBER;
import capsule.client.CapsulePreviewHandler;
import capsule.client.ClientConfig;
import capsule.client.render.CapsuleStateProperty;
import capsule.client.render.CapsuleTintSource;
import capsule.gui.LabelGui;
import capsule.items.CapsuleItem;
import capsule.items.CapsuleItems;
import fuzs.forgeconfigapiport.fabric.api.neoforge.v4.NeoForgeConfigRegistry;
import fuzs.forgeconfigapiport.fabric.api.neoforge.v4.NeoForgeModConfigEvents;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.recipe.v1.sync.ClientRecipeSynchronizedEvent;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
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
        NeoForgeModConfigEvents.loading(CapsuleMod.MODID).register(modConfig -> {
            if (modConfig.getSpec() == config) ClientConfig.bakeConfig();
        });
        NeoForgeModConfigEvents.reloading(CapsuleMod.MODID).register(modConfig -> {
            if (modConfig.getSpec() == config) ClientConfig.bakeConfig();
        });
        NeoForgeConfigRegistry.INSTANCE.register(CapsuleMod.MODID, ModConfig.Type.CLIENT, config);

        CapsuleMod.openGuiScreenCommon = LabelGui::open;
        FabricPayloadRegistrar.registerClientReceivers();

        // read by assets/capsule/items/capsule.json; both id mappers are opened by Fabric API's transitive class tweaker
        RangeSelectItemModelProperties.ID_MAPPER.put(CapsuleStateProperty.ID, CapsuleStateProperty.MAP_CODEC);
        ItemTintSources.ID_MAPPER.put(CapsuleTintSource.ID, CapsuleTintSource.MAP_CODEC);
        BlockEntityRenderers.register(CapsuleBlocks.MARKER_TE.get(), CaptureBER::new);

        WorldRenderEvents.AFTER_TRANSLUCENT.register(context -> CapsulePreviewHandler.onLevelRenderLast(
                context.matrixStack(), context.tickCounter().getGameTimeDeltaPartialTick(true)));
        ClientRecipeSynchronizedEvent.EVENT.register((client, recipes) -> CapsuleItems.registerRecipesClient(recipes.getAllOfType(RecipeType.CRAFTING)));
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> CapsulePreviewHandler.onChatMessage(message));
        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            if (client.player != null) CapsulePreviewHandler.onLocalPlayerTick(client.player);
        });
    }
}
