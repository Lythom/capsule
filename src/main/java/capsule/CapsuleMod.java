package capsule;

import capsule.blocks.CapsuleBlocks;
import capsule.command.CapsuleCommand;
import capsule.enchantments.Enchantments;
import capsule.items.CapsuleItem;
import capsule.items.CapsuleItems;
import capsule.network.CapsuleNetwork;
import capsule.recipes.CapsuleRecipes;
import capsule.itemGroups.CapsuleItemGroups;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraft.tileentity.TileEntityType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RecipesUpdatedEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import org.apache.logging.log4j.LogManager;

import java.util.function.Consumer;

@Mod(CapsuleMod.MODID)
public class CapsuleMod {
    protected static final org.apache.logging.log4j.Logger LOGGER = LogManager.getLogger(CapsuleMod.class);

    public static final String MODID = "capsule";
    public static ItemGroup tabCapsule = new CapsuleItemGroups(ItemGroup.getGroupCountSafe(), "capsule");

    public static Consumer<PlayerEntity> openGuiScreenCommon = DistExecutor.runForDist(() -> () -> CapsuleMod::openGuiScreenClient, () -> () -> CapsuleMod::openGuiScreenServer);

    @OnlyIn(Dist.CLIENT)
    public static void openGuiScreenClient(PlayerEntity player) {
        capsule.gui.LabelGui screen = new capsule.gui.LabelGui(player);
        Minecraft.getInstance().displayGuiScreen(screen);
    }

    public static void openGuiScreenServer(PlayerEntity player) {
    }
}

@Mod.EventBusSubscriber(modid = CapsuleMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
final class CapsuleModeEventSubscriber {

    @SubscribeEvent
    public static void setup(FMLCommonSetupEvent event) {
        Config.setup();
        CapsuleNetwork.setup();
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
        // TODO remove me if not needed: MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGH, Main::registerRecipes);
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

    @SubscribeEvent
    public static void serverStarting(FMLServerStartingEvent e) {
        CapsuleCommand.register(e.getCommandDispatcher());
    }
}

@Mod.EventBusSubscriber(modid = CapsuleMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
final class CapsuleForgeSubscriber {
    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public static void registerRecipes(RecipesUpdatedEvent event) {
        CapsuleItems.registerRecipesClient(event.getRecipeManager());
    }
}
