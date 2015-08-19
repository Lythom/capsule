package capsule.items;

import capsule.Main;
import capsule.items.recipes.DyeCapsuleRecipe;
import capsule.items.recipes.RecoveryCapsuleRecipe;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagInt;
import net.minecraftforge.fml.common.registry.GameRegistry;

public class CapsuleItemsRegistrer {
	
	public static Item capsule;
	public static Item creativeTP;
	
	public static void createItems(String modid) {
		GameRegistry.registerItem(creativeTP = new CreativeTP("capsule_CTP"), "capsule_CTP");
		creativeTP.setCreativeTab(Main.tabCapsule);
		GameRegistry.registerItem(capsule = new CapsuleItem("capsule"), "capsule");
		capsule.setCreativeTab(Main.tabCapsule);
		
		ModelBakery.addVariantName(capsule, modid+":capsule_empty", modid+":capsule_activated", modid+":capsule_linked", modid+":capsule_deployed", modid+":capsule_empty_activated", modid+":capsule_one_use", modid+":capsule_one_use_activated");
		//ModelBakery.addVariantName(creativeTP, modid+":capsule_CTP");
    }
	
	public static void registerRenderers(String modid) {
		Minecraft.getMinecraft().getRenderItem().getItemModelMesher().register(CapsuleItemsRegistrer.capsule, CapsuleItem.STATE_EMPTY, new ModelResourceLocation(modid+":capsule_empty", "inventory"));
		Minecraft.getMinecraft().getRenderItem().getItemModelMesher().register(CapsuleItemsRegistrer.capsule, CapsuleItem.STATE_ACTIVATED, new ModelResourceLocation(modid+":capsule_activated", "inventory"));
		Minecraft.getMinecraft().getRenderItem().getItemModelMesher().register(CapsuleItemsRegistrer.capsule, CapsuleItem.STATE_LINKED, new ModelResourceLocation(modid+":capsule_linked", "inventory"));
		Minecraft.getMinecraft().getRenderItem().getItemModelMesher().register(CapsuleItemsRegistrer.capsule, CapsuleItem.STATE_DEPLOYED, new ModelResourceLocation(modid+":capsule_deployed", "inventory"));
		Minecraft.getMinecraft().getRenderItem().getItemModelMesher().register(CapsuleItemsRegistrer.capsule, CapsuleItem.STATE_EMPTY_ACTIVATED, new ModelResourceLocation(modid+":capsule_empty_activated", "inventory"));
		Minecraft.getMinecraft().getRenderItem().getItemModelMesher().register(CapsuleItemsRegistrer.capsule, CapsuleItem.STATE_ONE_USE, new ModelResourceLocation(modid+":capsule_one_use", "inventory"));
		Minecraft.getMinecraft().getRenderItem().getItemModelMesher().register(CapsuleItemsRegistrer.capsule, CapsuleItem.STATE_ONE_USE_ACTIVATED, new ModelResourceLocation(modid+":capsule_one_use_activated", "inventory"));
		
		Minecraft.getMinecraft().getRenderItem().getItemModelMesher().register(CapsuleItemsRegistrer.creativeTP, 0, new ModelResourceLocation(modid+":capsule_CTP", "inventory"));
	}
	
	public static void registerRecipes() {
		ItemStack ironCapsule = new ItemStack(CapsuleItemsRegistrer.capsule, 1, CapsuleItem.STATE_EMPTY);
		ironCapsule.setTagInfo("color", new NBTTagInt(0xCCCCCC));
		ironCapsule.setTagInfo("size", new NBTTagInt(3));
		
		ItemStack goldCapsule = new ItemStack(CapsuleItemsRegistrer.capsule, 1, CapsuleItem.STATE_EMPTY);
		goldCapsule.setTagInfo("color", new NBTTagInt(0xFFD700));
		goldCapsule.setTagInfo("size", new NBTTagInt(5));
		
		ItemStack diamondCapsule = new ItemStack(CapsuleItemsRegistrer.capsule, 1, CapsuleItem.STATE_EMPTY);
		diamondCapsule.setTagInfo("color", new NBTTagInt(0x00FFF2));
		diamondCapsule.setTagInfo("size", new NBTTagInt(7));
		
		// base recipes
		GameRegistry.addRecipe(ironCapsule, new Object[] {" B ", "#P#", " # ", '#', Items.iron_ingot, 'P', Items.ender_pearl, 'B', Blocks.stone_button});
		GameRegistry.addRecipe(goldCapsule, new Object[] {" B ", "#P#", " # ", '#', Items.gold_ingot, 'P', Items.ender_pearl, 'B', Blocks.stone_button});
		GameRegistry.addRecipe(diamondCapsule, new Object[] {" B ", "#P#", " # ", '#', Items.diamond, 'P', Items.ender_pearl, 'B', Blocks.stone_button});
		
		// recovery capsule recipe
		GameRegistry.addRecipe(new RecoveryCapsuleRecipe(new ItemStack(CapsuleItemsRegistrer.capsule, 1, CapsuleItem.STATE_LINKED), new ItemStack(Items.glass_bottle), CapsuleItem.STATE_ONE_USE));
		
		// dye recipe
		GameRegistry.addRecipe(new DyeCapsuleRecipe());
		
	}
}
