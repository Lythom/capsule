package capsule;

import capsule.blocks.CapsuleBlocks;
import capsule.command.CapsuleCommand;
import capsule.enchantments.CapsuleEnchantments;
import capsule.itemGroups.CapsuleCreativeTabs;
import capsule.items.CapsuleItem;
import capsule.items.CapsuleItems;
import capsule.network.CapsuleNetwork;
import capsule.recipes.CapsuleRecipes;
import capsule.recipes.PrefabsBlueprintAggregatorRecipe;
import capsule.structure.CapsuleTemplateManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RecipesUpdatedEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import org.apache.logging.log4j.LogManager;

import java.util.function.Consumer;

@Mod(CapsuleMod.MODID)
public class CapsuleMod {
    protected static final org.apache.logging.log4j.Logger LOGGER = LogManager.getLogger(CapsuleMod.class);

    public static final String MODID = "capsule";

    public static Consumer<Player> openGuiScreenCommon;
    public static MinecraftServer server = null;

    static {
        if (FMLLoader.getDist() == Dist.CLIENT) {
            openGuiScreenCommon = CapsuleMod::openGuiScreenClient;
        } else {
            openGuiScreenCommon = CapsuleMod::openGuiScreenServer;
        }
    }

    public CapsuleMod(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.COMMON_CONFIG);

        CapsuleBlocks.registerBlocks(modEventBus);
        CapsuleItems.registerItems(modEventBus);
        CapsuleCreativeTabs.registerTabs(modEventBus);
        CapsuleRecipes.registerRecipeSerializers(modEventBus);

        modEventBus.addListener(CapsuleNetwork::setupPackets);

        NeoForge.EVENT_BUS.addListener(EventPriority.HIGH, CapsuleMod::serverStarting);
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGH, CapsuleMod::serverStopped);
        NeoForge.EVENT_BUS.addListener(EventPriority.NORMAL, CapsuleMod::RegisterCommands);
    }

    public static void serverStarting(final ServerStartingEvent e) {
        server = e.getServer();
        Config.populateConfigFolders(server);
        if (PrefabsBlueprintAggregatorRecipe.instance != null)
            PrefabsBlueprintAggregatorRecipe.instance.populateRecipes(CapsuleMod.server.getResourceManager());
    }

    public static void RegisterCommands(final RegisterCommandsEvent e) {
        CapsuleCommand.register(e.getDispatcher());
    }

    public static void serverStopped(final ServerStoppedEvent e) {
        server = null;
    }

    @OnlyIn(Dist.CLIENT)
    public static void openGuiScreenClient(Player player) {
        if (player.level().isClientSide) {
            capsule.gui.LabelGui screen = new capsule.gui.LabelGui(player);
            Minecraft.getInstance().setScreen(screen);
        }
    }

    public static void openGuiScreenServer(Player player) {
    }
}

@EventBusSubscriber(modid = CapsuleMod.MODID, bus = EventBusSubscriber.Bus.MOD)
final class CapsuleModEventSubscriber {

    /**
     * This method will be called by Forge when a config changes.
     */
    @SubscribeEvent
    public static void onModConfigEvent(final ModConfigEvent event) {
        final ModConfig config = event.getConfig();
        // Rebake the configs when they change
        if (config.getSpec() == Config.COMMON_CONFIG) {
            Config.bakeConfig(config);
            CapsuleMod.LOGGER.debug("Baked COMMON_CONFIG");
        }
    }
}

@EventBusSubscriber(modid = CapsuleMod.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
final class CapsuleClientModEventSubscriber {

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> ItemProperties.register(
                CapsuleItems.CAPSULE.get(),
                ResourceLocation.fromNamespaceAndPath(CapsuleMod.MODID, "state"),
                (stack, world, entity, seed) -> CapsuleItem.getState(stack).getValue()
        ));
    }

    @SubscribeEvent
    public static void registerColor(RegisterColorHandlersEvent.Item event) {
        event.register((stack, tintIndex) -> {
            if (stack.getItem() instanceof CapsuleItem) {
                return CapsuleItem.getColorFromItemstack(stack, tintIndex);
            }
            return 0xFFFFFFFF;
        }, CapsuleItems.CAPSULE.get());
    }

    @SubscribeEvent
    public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        CapsuleBlocks.registerBlockEntitiesRenderer(event);
    }
}

@EventBusSubscriber(modid = CapsuleMod.MODID, value = Dist.CLIENT)
final class CapsuleForgeSubscriber {
    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public static void registerRecipes(RecipesUpdatedEvent event) {
        CapsuleItems.registerRecipesClient(event.getRecipeManager());
    }

    @SubscribeEvent
    public static void setup(AddReloadListenerEvent event) {
        event.addListener(new StructureSaverReloadListener());
    }
}

class StructureSaverReloadListener extends SimplePreparableReloadListener<Void> {
    protected Void prepare(ResourceManager pResourceManager, ProfilerFiller pProfiler) {
        StructureSaver.getRewardManager(pResourceManager).onResourceManagerReload(pResourceManager);
        for (CapsuleTemplateManager ctm : StructureSaver.CapsulesManagers.values()) {
            ctm.onResourceManagerReload(pResourceManager);
        }
        return (Void) null;
    }

    protected void apply(Void pObject, ResourceManager pResourceManager, ProfilerFiller pProfiler) {

    }
}
