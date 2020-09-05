package capsule.client;

import capsule.CommonProxy;
import capsule.Main;
import capsule.blocks.CapsuleBlocks;
import capsule.blocks.CaptureTESR;
import capsule.blocks.TileEntityCapture;
import capsule.items.CapsuleItem;
import capsule.items.CapsuleItems;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;


@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class ClientProxy extends CommonProxy {

    public void setup(FMLCommonSetupEvent event) {
        super.setup(event);
    }

    public void init(FMLClientSetupEvent event) {
        CapsulePreviewHandler cph = new CapsulePreviewHandler();
        // for the undeploy preview
        MinecraftForge.EVENT_BUS.register(cph);

        // register color variants
        Minecraft.getInstance().getItemColors().register((stack, tintIndex) -> {
            if (stack.getItem() instanceof CapsuleItem) {
                return CapsuleItem.getColorFromItemstack(stack, tintIndex);
            }
            return 0xFFFFFF;
        }, CapsuleItems.capsule);
    }

    @SubscribeEvent
    public static void registerModels(ModelRegistryEvent event) {
        registerItemRenderers(Main.MODID);
        registerBlockRenderer(Main.MODID);
    }

    private static void registerBlockRenderer(String modid) {
        ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(CapsuleBlocks.blockCapsuleMarker), 0,
                new ModelResourceLocation(modid + ":capsulemarker", "inventory"));
        ClientRegistry.bindTileEntitySpecialRenderer(TileEntityCapture.class, new CaptureTESR());
    }

    public static void registerItemRenderers(String modid) {

        ModelLoader.setCustomModelResourceLocation(CapsuleItems.capsule, CapsuleItem.STATE_EMPTY,
                new ModelResourceLocation(modid + ":capsule_empty", "inventory"));
        ModelLoader.setCustomModelResourceLocation(CapsuleItems.capsule, CapsuleItem.STATE_ACTIVATED,
                new ModelResourceLocation(modid + ":capsule_activated", "inventory"));
        ModelLoader.setCustomModelResourceLocation(CapsuleItems.capsule, CapsuleItem.STATE_LINKED,
                new ModelResourceLocation(modid + ":capsule_linked", "inventory"));
        ModelLoader.setCustomModelResourceLocation(CapsuleItems.capsule, CapsuleItem.STATE_DEPLOYED,
                new ModelResourceLocation(modid + ":capsule_deployed", "inventory"));
        ModelLoader.setCustomModelResourceLocation(CapsuleItems.capsule, CapsuleItem.STATE_EMPTY_ACTIVATED,
                new ModelResourceLocation(modid + ":capsule_empty_activated", "inventory"));
        ModelLoader.setCustomModelResourceLocation(CapsuleItems.capsule, CapsuleItem.STATE_ONE_USE,
                new ModelResourceLocation(modid + ":capsule_one_use", "inventory"));
        ModelLoader.setCustomModelResourceLocation(CapsuleItems.capsule, CapsuleItem.STATE_ONE_USE_ACTIVATED,
                new ModelResourceLocation(modid + ":capsule_one_use_activated", "inventory"));
        ModelLoader.setCustomModelResourceLocation(CapsuleItems.capsule, CapsuleItem.STATE_BLUEPRINT,
                new ModelResourceLocation(modid + ":capsule_blueprint", "inventory"));

        // Item renderer
        ModelBakery.registerItemVariants(CapsuleItems.capsule, new ResourceLocation(modid + ":capsule_empty"),
                new ResourceLocation(modid + ":capsule_activated"), new ResourceLocation(modid + ":capsule_linked"),
                new ResourceLocation(modid + ":capsule_deployed"), new ResourceLocation(modid + ":capsule_empty_activated"),
                new ResourceLocation(modid + ":capsule_one_use"), new ResourceLocation(modid + ":capsule_one_use_activated"),
                new ResourceLocation(modid + ":capsule_blueprint"));
    }

    public void openGuiScreen(PlayerEntity playerIn) {
        capsule.gui.LabelGui screen = new capsule.gui.LabelGui(playerIn);
        Minecraft.getMinecraft().displayGuiScreen(screen);
    }

}
