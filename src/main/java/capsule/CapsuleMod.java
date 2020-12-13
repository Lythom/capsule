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
import net.minecraft.block.Block;
import net.minecraft.block.DispenserBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntityType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RecipesUpdatedEvent;
import net.minecraftforge.common.MinecraftForge;
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
    public static ItemGroup tabCapsule = new CapsuleItemGroups(ItemGroup.getGroupCountSafe(), "capsule");

    public static Consumer<PlayerEntity> openGuiScreenCommon = DistExecutor.safeRunForDist(() -> () -> CapsuleMod::openGuiScreenClient, () -> () -> CapsuleMod::openGuiScreenServer);
    public static MinecraftServer server = null;

    public CapsuleMod() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.COMMON_CONFIG);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGH, CapsuleMod::serverStarting);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGH, CapsuleMod::serverStopped);
    }

    public static void serverStarting(final FMLServerStartingEvent e) {
        CapsuleCommand.register(e.getCommandDispatcher());
        server = e.getServer();
        Config.populateConfigFolders(server);
        if (PrefabsBlueprintAggregatorRecipe.instance != null) PrefabsBlueprintAggregatorRecipe.instance.populateRecipes(CapsuleMod.server);
    }

    public static void serverStopped(final FMLServerStoppedEvent e) {
        server = null;
    }

    @OnlyIn(Dist.CLIENT)
    public static void openGuiScreenClient(PlayerEntity player) {
        capsule.gui.LabelGui screen = new capsule.gui.LabelGui(player);
        Minecraft.getInstance().displayGuiScreen(screen);
    }

    public static void openGuiScreenServer(PlayerEntity player) {
    }
}

@Mod.EventBusSubscriber(modid = CapsuleMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
final class CapsuleModEventSubscriber {

    @SubscribeEvent
    public static void setup(FMLCommonSetupEvent event) {
        CapsuleNetwork.setup();
        DispenserBlock.registerDispenseBehavior(CapsuleItems.CAPSULE, new DispenseCapsuleBehavior());
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
    public static void registerTileEntities(final RegistryEvent.Register<TileEntityType<?>> event) {
        CapsuleBlocks.registerTileEntities(event);
    }

    @SubscribeEvent
    public static void registerEnchantments(RegistryEvent.Register<Enchantment> event) {
        Enchantments.registerEnchantments(event);
    }

    @SubscribeEvent
    public static void registerRecipeSerializers(RegistryEvent.Register<IRecipeSerializer<?>> event) {
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
}
