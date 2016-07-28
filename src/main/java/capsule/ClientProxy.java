package capsule;

import capsule.blocks.CapsuleBlocksRegistrer;
import capsule.blocks.CaptureTESR;
import capsule.blocks.TileEntityCapture;
import capsule.items.CapsuleItem;
import capsule.items.CapsuleItemsRegistrer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemModelMesher;
import net.minecraft.client.renderer.block.model.ModelBakery;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.color.IItemColor;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

public class ClientProxy extends CommonProxy {

	public void preInit(FMLPreInitializationEvent event) {
		super.preInit(event);
		String modid = Main.MODID;

		// Item renderer
		ModelBakery.registerItemVariants(CapsuleItemsRegistrer.capsule, new ResourceLocation(modid + ":capsule_empty"),
				new ResourceLocation(modid + ":capsule_activated"), new ResourceLocation(modid + ":capsule_linked"),
				new ResourceLocation(modid + ":capsule_deployed"), new ResourceLocation(modid + ":capsule_empty_activated"),
				new ResourceLocation(modid + ":capsule_one_use"), new ResourceLocation(modid + ":capsule_one_use_activated"));
	}

	public void init(FMLInitializationEvent event) {
		super.init(event);
		registerItemRenderers(Main.MODID);

		// block renderers
		registerBlockRenderer();

		CapsulePreviewHandler cph = new CapsulePreviewHandler();
		// for the undeploy preview
		MinecraftForge.EVENT_BUS.register(cph);
		
		
	}

	private void registerBlockRenderer() {
		ItemModelMesher mesher = Minecraft.getMinecraft().getRenderItem().getItemModelMesher();
		mesher.register(Item.getItemFromBlock(CapsuleBlocksRegistrer.blockCapsuleMarker), 0,
				new ModelResourceLocation(Main.MODID + ":capsulemarker", "inventory"));
		ClientRegistry.bindTileEntitySpecialRenderer(TileEntityCapture.class, new CaptureTESR());
	}

	public void postInit(FMLPostInitializationEvent event) {
		super.postInit(event);
	}

	public void openGuiScreen(EntityPlayer playerIn) {
		capsule.gui.LabelGui screen = new capsule.gui.LabelGui(playerIn);
		Minecraft.getMinecraft().displayGuiScreen(screen);
	}

	public static void registerItemRenderers(String modid) {

		Minecraft.getMinecraft().getRenderItem().getItemModelMesher().register(CapsuleItemsRegistrer.capsule, CapsuleItem.STATE_EMPTY,
				new ModelResourceLocation(modid + ":capsule_empty", "inventory"));
		Minecraft.getMinecraft().getRenderItem().getItemModelMesher().register(CapsuleItemsRegistrer.capsule, CapsuleItem.STATE_ACTIVATED,
				new ModelResourceLocation(modid + ":capsule_activated", "inventory"));
		Minecraft.getMinecraft().getRenderItem().getItemModelMesher().register(CapsuleItemsRegistrer.capsule, CapsuleItem.STATE_LINKED,
				new ModelResourceLocation(modid + ":capsule_linked", "inventory"));
		Minecraft.getMinecraft().getRenderItem().getItemModelMesher().register(CapsuleItemsRegistrer.capsule, CapsuleItem.STATE_DEPLOYED,
				new ModelResourceLocation(modid + ":capsule_deployed", "inventory"));
		Minecraft.getMinecraft().getRenderItem().getItemModelMesher().register(CapsuleItemsRegistrer.capsule, CapsuleItem.STATE_EMPTY_ACTIVATED,
				new ModelResourceLocation(modid + ":capsule_empty_activated", "inventory"));
		Minecraft.getMinecraft().getRenderItem().getItemModelMesher().register(CapsuleItemsRegistrer.capsule, CapsuleItem.STATE_ONE_USE,
				new ModelResourceLocation(modid + ":capsule_one_use", "inventory"));
		Minecraft.getMinecraft().getRenderItem().getItemModelMesher().register(CapsuleItemsRegistrer.capsule, CapsuleItem.STATE_ONE_USE_ACTIVATED,
				new ModelResourceLocation(modid + ":capsule_one_use_activated", "inventory"));

		Minecraft.getMinecraft().getItemColors().registerItemColorHandler(new IItemColor() {
			public int getColorFromItemstack(ItemStack stack, int tintIndex) {
				if (stack.getItem() instanceof CapsuleItem) {
					return ((CapsuleItem) stack.getItem()).getColorFromItemstack(stack, tintIndex);
				}
				return 0xFFFFFF;
			}
		}, CapsuleItemsRegistrer.capsule);
	}

}
