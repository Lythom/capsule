package capsule.neoforge;

import capsule.CapsuleMod;
import capsule.Config;
import capsule.StructureSaver;
import capsule.enchantments.RecallEnchant;
import capsule.items.CapsuleItem;
import capsule.items.ThrownCapsules;
import capsule.loot.CapsuleLootTableHook;
import capsule.loot.StarterLoot;
import capsule.network.CapsuleNetwork;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.loot.LootPool;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.LootTableLoadEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

@Mod(CapsuleMod.MODID)
public class CapsuleNeoForge {

    public CapsuleNeoForge(IEventBus modEventBus, ModContainer modContainer, Dist dist) {
        ModConfigSpec config = ModConfigSpecBuilder.build(Config::define);
        modContainer.registerConfig(ModConfig.Type.COMMON, config);
        modEventBus.addListener((ModConfigEvent event) -> {
            if (event.getConfig().getSpec() == config) Config.bakeConfig();
        });

        CapsuleMod.init();
        NeoForgePlatform.registerAll(modEventBus);
        modEventBus.addListener((RegisterPayloadHandlersEvent event) ->
                CapsuleNetwork.registerPayloads(new NeoForgePayloadRegistrar(event.registrar(CapsuleMod.MODID).versioned(CapsuleNetwork.VERSION))));

        IEventBus bus = NeoForge.EVENT_BUS;
        bus.addListener(EventPriority.HIGH, (ServerStartingEvent event) -> CapsuleMod.serverStarting(event.getServer()));
        bus.addListener(EventPriority.HIGH, (ServerStoppedEvent event) -> CapsuleMod.serverStopped());
        bus.addListener((RegisterCommandsEvent event) -> CapsuleMod.registerCommands(event.getDispatcher()));
        bus.addListener((AddReloadListenerEvent event) -> event.addListener(CapsuleMod.reloadListener()));
        bus.addListener(EventPriority.HIGHEST, (EntityJoinLevelEvent event) -> {
            if (StructureSaver.isPreventedDrop(event.getEntity())) event.setCanceled(true);
        });
        bus.addListener(EventPriority.LOWEST, (EntityJoinLevelEvent event) -> ThrownCapsules.onEntityLoad(event.getEntity(), event.getLevel()));
        bus.addListener((EntityLeaveLevelEvent event) -> ThrownCapsules.onEntityUnload(event.getEntity(), event.getLevel()));
        bus.addListener((LootTableLoadEvent event) -> {
            LootPool.Builder pool = CapsuleLootTableHook.capsulePool(event.getName());
            if (pool != null) event.getTable().addPool(pool.name("capsulePool").build());
        });
        bus.addListener(EventPriority.LOWEST, (PlayerEvent.PlayerLoggedInEvent event) -> {
            if (event.getEntity() instanceof ServerPlayer player) StarterLoot.playerLogin(player);
        });
        bus.addListener((PlayerTickEvent.Post event) -> CapsuleItem.onPlayerTick(event.getEntity()));
        bus.addListener((LevelTickEvent.Post event) -> {
            if (event.getLevel() instanceof ServerLevel level) RecallEnchant.onWorldTickEvent(level);
        });
        bus.addListener((PlayerInteractEvent.LeftClickBlock event) -> {
            if (!event.isCanceled() && CapsuleItem.onLeftClickBlock(event.getEntity(), event.getLevel())) event.setCanceled(true);
        });

        if (dist.isClient()) {
            CapsuleNeoForgeClient.init(modEventBus, modContainer);
        }
    }
}
