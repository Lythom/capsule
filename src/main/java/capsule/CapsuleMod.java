package capsule;

import capsule.blocks.CapsuleBlocks;
import capsule.command.CapsuleCommand;
import capsule.dispenser.DispenseCapsuleBehavior;
import capsule.enchantments.Enchantments;
import capsule.itemGroups.CapsuleItemGroups;
import capsule.items.CapsuleItem;
import capsule.items.CapsuleItems;
import capsule.network.CapsuleNetwork;
import capsule.recipes.CapsuleRecipes;
import capsule.recipes.PrefabsBlueprintAggregatorRecipe;
import capsule.structure.CapsuleTemplateManager;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RecipesUpdatedEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import net.minecraftforge.fml.event.server.FMLServerStoppedEvent;
import org.apache.logging.log4j.LogManager;

import java.util.function.Consumer;

@Mod(CapsuleMod.MODID)
public class CapsuleMod {
    protected static final org.apache.logging.log4j.Logger LOGGER = LogManager.getLogger(CapsuleMod.class);

    public static final String MODID = "capsule";
    public static CreativeModeTab tabCapsule = new CapsuleItemGroups(CreativeModeTab.getGroupCountSafe(), "capsule");

    public static Consumer<Player> openGuiScreenCommon = DistExecutor.runForDist(() -> () -> CapsuleMod::openGuiScreenClient, () -> () -> CapsuleMod::openGuiScreenServer);
    public static MinecraftServer server = null;

    public CapsuleMod() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.COMMON_CONFIG);
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, Config.CLIENT_CONFIG);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGH, CapsuleMod::serverStarting);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGH, CapsuleMod::serverStopped);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, CapsuleMod::RegisterCommands);
    }

    public static void serverStarting(final FMLServerStartingEvent e) {
        server = e.getServer();
        Config.populateConfigFolders(server);
        if (PrefabsBlueprintAggregatorRecipe.instance != null)
            PrefabsBlueprintAggregatorRecipe.instance.populateRecipes(CapsuleMod.server.getResourceManager());
    }

    public static void RegisterCommands(final RegisterCommandsEvent e) {
        CapsuleCommand.register(e.getDispatcher());
    }

    public static void serverStopped(final FMLServerStoppedEvent e) {
        server = null;
    }

    @OnlyIn(Dist.CLIENT)
    public static void openGuiScreenClient(Player player) {
        capsule.gui.LabelGui screen = new capsule.gui.LabelGui(player);
        Minecraft.getInstance().setScreen(screen);
    }

    public static void openGuiScreenServer(Player player) {
    }
}

@Mod.EventBusSubscriber(modid = CapsuleMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
final class CapsuleModEventSubscriber {

    @SubscribeEvent
    public static void setup(FMLCommonSetupEvent event) {
        CapsuleNetwork.setup();
        DispenserBlock.registerBehavior(CapsuleItems.CAPSULE, new DispenseCapsuleBehavior());
    }

    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public static void clientSetup(FMLClientSetupEvent event) {
        CapsuleBlocks.bindTileEntitiesRenderer();

        // register color variants
        Minecraft.getInstance().getItemColors().register((stack, tintIndex) -> {
            if (stack.getItem() instanceof CapsuleItem) {
                return CapsuleItem.getColorFromItemstack(stack, tintIndex);
            }
            return 0xFFFFFF;
        }, CapsuleItems.CAPSULE);

        ItemProperties.register(
                CapsuleItems.CAPSULE,
                new ResourceLocation(CapsuleMod.MODID, "state"),
                (stack, world, entity) -> CapsuleItem.getState(stack).getValue()
        );
    }

    /**
     * This method will be called by Forge when a config changes.
     */
    @SubscribeEvent
    public static void onModConfigEvent(final ModConfig.ModConfigEvent event) {
        final ModConfig config = event.getConfig();
        // Rebake the configs when they change
        if (config.getSpec() == Config.COMMON_CONFIG) {
            Config.bakeConfig(config);
            CapsuleMod.LOGGER.debug("Baked COMMON_CONFIG");
        }
        if (config.getSpec() == Config.CLIENT_CONFIG) {
            Config.bakeClientConfig(config);
            CapsuleMod.LOGGER.debug("Baked CLIENT_CONFIG");
        }
    }

    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> event) {
        CapsuleBlocks.registerBlocks(event);
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        CapsuleBlocks.registerItemBlocks(event);
        CapsuleItems.registerItems(event);
    }

    @SubscribeEvent
    public static void registerTileEntities(final RegistryEvent.Register<BlockEntityType<?>> event) {
        CapsuleBlocks.registerTileEntities(event);
    }

    @SubscribeEvent
    public static void registerEnchantments(RegistryEvent.Register<Enchantment> event) {
        Enchantments.registerEnchantments(event);
    }

    @SubscribeEvent
    public static void registerRecipeSerializers(RegistryEvent.Register<RecipeSerializer<?>> event) {
        CapsuleRecipes.registerRecipeSerializers(event);
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
        StructureSaver.getRewardManager(event.getResourceManager()).onResourceManagerReload(event.getResourceManager());
        for (CapsuleTemplateManager ctm : StructureSaver.CapsulesManagers.values()) {
            ctm.onResourceManagerReload(event.getResourceManager());
        }

    }
}
