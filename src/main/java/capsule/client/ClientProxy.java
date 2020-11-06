package capsule.client;

import capsule.CommonProxy;
import capsule.Main;
import capsule.blocks.CaptureTESR;
import capsule.items.CapsuleItem;
import capsule.items.CapsuleItems;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.event.RecipesUpdatedEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = Main.MODID, bus = Bus.MOD)
public class ClientProxy extends CommonProxy {

    @SubscribeEvent
    public static void init(FMLClientSetupEvent event) {
        CapsulePreviewHandler cph = new CapsulePreviewHandler();
        // for the undeploy preview
        MinecraftForge.EVENT_BUS.register(cph);

        // register color variants
        Minecraft.getInstance().getItemColors().register((stack, tintIndex) -> {
            if (stack.getItem() instanceof CapsuleItem) {
                return CapsuleItem.getColorFromItemstack(stack, tintIndex);
            }
            return 0xFFFFFF;
        }, CapsuleItems.CAPSULE);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGH, ClientProxy::registerRecipes);
    }

    public static void registerRecipes(RecipesUpdatedEvent event) {
        CapsuleItems.registerRecipesClient(event.getRecipeManager());
    }

    @SubscribeEvent
    public static void registerModels(ModelRegistryEvent event) {
        registerBlockRenderer();
    }

    private static void registerBlockRenderer() {
        ClientRegistry.bindTileEntityRenderer(CommonProxy.MARKER_TE, CaptureTESR::new);
    }

    public void openGuiScreen(PlayerEntity playerIn) {
        capsule.gui.LabelGui screen = new capsule.gui.LabelGui(playerIn);
        Minecraft.getInstance().displayGuiScreen(screen);
    }

}
