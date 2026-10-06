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
import fuzs.forgeconfigapiport.fabric.api.neoforge.v4.NeoForgeConfigRegistry;
import fuzs.forgeconfigapiport.fabric.api.neoforge.v4.NeoForgeModConfigEvents;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.item.v1.EnchantmentEvents;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.storage.loot.LootPool;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class CapsuleFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        ModConfigSpec config = ModConfigSpecBuilder.build(Config::define);
        NeoForgeModConfigEvents.loading(CapsuleMod.MODID).register(modConfig -> {
            if (modConfig.getSpec() == config) Config.bakeConfig();
        });
        NeoForgeModConfigEvents.reloading(CapsuleMod.MODID).register(modConfig -> {
            if (modConfig.getSpec() == config) Config.bakeConfig();
        });
        NeoForgeConfigRegistry.INSTANCE.register(CapsuleMod.MODID, ModConfig.Type.COMMON, config);

        CapsuleMod.init();
        CapsuleNetwork.registerPayloads(new FabricPayloadRegistrar());

        ServerLifecycleEvents.SERVER_STARTING.register(CapsuleMod::serverStarting);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> CapsuleMod.serverStopped());
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> CapsuleMod.registerCommands(dispatcher));
        ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(new TemplatesReloadListener(CapsuleMod.reloadListener()));
        LootTableEvents.MODIFY.register((key, tableBuilder, source, registries) -> {
            LootPool.Builder pool = CapsuleLootTableHook.capsulePool(key.location());
            if (pool != null) tableBuilder.withPool(pool);
        });
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> StarterLoot.playerLogin(handler.player));
        ServerTickEvents.END_SERVER_TICK.register(server -> server.getPlayerList().getPlayers().forEach(CapsuleItem::onPlayerTick));
        ServerTickEvents.END_WORLD_TICK.register(RecallEnchant::onWorldTickEvent);
        ServerEntityEvents.ENTITY_LOAD.register(ThrownCapsules::onEntityLoad);
        ServerEntityEvents.ENTITY_UNLOAD.register(ThrownCapsules::onEntityUnload);
        EnchantmentEvents.ALLOW_ENCHANTING.register((enchantment, target, context) ->
                CapsuleEnchantments.acceptsLoyalty(target, enchantment) ? TriState.TRUE : TriState.DEFAULT);
        AttackBlockCallback.EVENT.register((player, level, hand, pos, direction) ->
                CapsuleItem.onLeftClickBlock(player, level) ? InteractionResult.FAIL : InteractionResult.PASS);
    }

    private record TemplatesReloadListener(PreparableReloadListener listener) implements IdentifiableResourceReloadListener {
        @Override
        public ResourceLocation getFabricId() {
            return ResourceLocation.fromNamespaceAndPath(CapsuleMod.MODID, "templates");
        }

        @Override
        public CompletableFuture<Void> reload(PreparationBarrier barrier, ResourceManager manager, ProfilerFiller preparationsProfiler,
                                              ProfilerFiller reloadProfiler, Executor backgroundExecutor, Executor gameExecutor) {
            return listener.reload(barrier, manager, preparationsProfiler, reloadProfiler, backgroundExecutor, gameExecutor);
        }
    }
}
