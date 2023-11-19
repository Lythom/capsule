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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RecipesUpdatedEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;

import java.util.function.Consumer;

@Mod(CapsuleMod.MODID)
public class CapsuleMod {
    protected static final org.apache.logging.log4j.Logger LOGGER = LogManager.getLogger(CapsuleMod.class);

    public static final String MODID = "capsule";

    public static Consumer<Player> openGuiScreenCommon = DistExecutor.unsafeRunForDist(() -> () -> CapsuleMod::openGuiScreenClient, () -> () -> CapsuleMod::openGuiScreenServer);
    public static MinecraftServer server = null;

    public CapsuleMod() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.COMMON_CONFIG);

        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        CapsuleBlocks.registerBlocks(modEventBus);
        CapsuleItems.registerItems(modEventBus);
        CapsuleCreativeTabs.registerTabs(modEventBus);
        CapsuleRecipes.registerRecipeSerializers(modEventBus);
        CapsuleEnchantments.registerEnchantments(modEventBus);

        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGH, CapsuleMod::serverStarting);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGH, CapsuleMod::serverStopped);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, CapsuleMod::RegisterCommands);
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

@Mod.EventBusSubscriber(modid = CapsuleMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
final class CapsuleModEventSubscriber {

    @SubscribeEvent
    public static void setup(FMLCommonSetupEvent event) {
        CapsuleNetwork.setup();
    }

    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public static void clientSetup(FMLClientSetupEvent event) {
        // register color variants
        Minecraft.getInstance().getItemColors().register((stack, tintIndex) -> {
            if (stack.getItem() instanceof CapsuleItem) {
                return CapsuleItem.getColorFromItemstack(stack, tintIndex);
            }
            return 0xFFFFFF;
        }, CapsuleItems.CAPSULE.get());
        event.enqueueWork(() -> ItemProperties.register(
                CapsuleItems.CAPSULE.get(),
                new ResourceLocation(CapsuleMod.MODID, "state"),
                (stack, world, entity, seed) -> CapsuleItem.getState(stack).getValue()
        ));
    }

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

    @SubscribeEvent
    public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        CapsuleBlocks.registerBlockEntitiesRenderer(event);
    }
}

@Mod.EventBusSubscriber(modid = CapsuleMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
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
