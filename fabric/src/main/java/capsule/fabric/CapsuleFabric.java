package capsule.fabric;

import capsule.CapsuleMod;
import capsule.Config;
import capsule.enchantments.CapsuleEnchantments;
import capsule.enchantments.RecallEnchant;
import capsule.items.CapsuleItem;
import capsule.items.ThrownCapsules;
import capsule.loot.CapsuleLootTableHook;
import capsule.loot.StarterLoot;
import capsule.network.CapsuleNetwork;
import capsule.plugins.claims.Claims;
import capsule.recipes.CapsuleRecipes;
import fuzs.forgeconfigapiport.fabric.api.v5.ConfigRegistry;
import fuzs.forgeconfigapiport.fabric.api.v5.ModConfigEvents;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLevelEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.item.v1.EnchantmentEvents;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.recipe.v1.sync.RecipeSynchronization;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.storage.loot.LootPool;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

public class CapsuleFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        ModConfigSpec config = ModConfigSpecBuilder.build(Config::define);
        ModConfigEvents.loading(CapsuleMod.MODID).register(modConfig -> {
            if (modConfig.getSpec() == config) Config.bakeConfig();
        });
        ModConfigEvents.reloading(CapsuleMod.MODID).register(modConfig -> {
            if (modConfig.getSpec() == config) Config.bakeConfig();
        });
        ConfigRegistry.INSTANCE.register(CapsuleMod.MODID, ModConfig.Type.COMMON, config);

        CapsuleMod.init();
        CapsuleNetwork.registerPayloads(new FabricPayloadRegistrar());
        // the creative tab, the recipe viewers and the prefab recipes are built from these recipes on the client
        CapsuleRecipes.clientSyncedSerializers().forEach(RecipeSynchronization::synchronizeRecipeSerializer);
        // the claim mod of Fabric only, CapsuleMod.serverStarting loads the others
        Claims.load("goml", () -> new GetOffMyLawnAdapter());

        ServerLifecycleEvents.SERVER_STARTING.register(CapsuleMod::serverStarting);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> CapsuleMod.serverStopped());
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> CapsuleMod.registerCommands(dispatcher));
        ResourceLoader.get(PackType.SERVER_DATA).registerReloadListener(Identifier.fromNamespaceAndPath(CapsuleMod.MODID, "templates"), CapsuleMod.reloadListener());
        LootTableEvents.MODIFY.register((key, tableBuilder, source, registries) -> {
            LootPool.Builder pool = CapsuleLootTableHook.capsulePool(key.identifier());
            if (pool != null) tableBuilder.withPool(pool);
        });
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> StarterLoot.playerLogin(handler.player));
        ServerTickEvents.END_SERVER_TICK.register(server -> server.getPlayerList().getPlayers().forEach(CapsuleItem::onPlayerTick));
        ServerTickEvents.END_LEVEL_TICK.register(RecallEnchant::onWorldTickEvent);
        ServerEntityEvents.ENTITY_LOAD.register(ThrownCapsules::onEntityLoad);
        ServerEntityEvents.ENTITY_UNLOAD.register(ThrownCapsules::onEntityUnload);
        ServerLevelEvents.UNLOAD.register((server, level) -> ThrownCapsules.onLevelUnload(level));
        EnchantmentEvents.ALLOW_ENCHANTING.register((enchantment, target, context) ->
                CapsuleEnchantments.acceptsLoyalty(target, enchantment) ? TriState.TRUE : TriState.DEFAULT);
        AttackBlockCallback.EVENT.register((player, level, hand, pos, direction) ->
                CapsuleItem.onLeftClickBlock(player, level) ? InteractionResult.FAIL : InteractionResult.PASS);
    }
}
