package capsule.items;

import capsule.items.recipes.DyeCapsuleRecipe;
import capsule.items.recipes.RepairCapsuleRecipe;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.client.resources.model.ModelResourceLocation;
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
		GameRegistry.registerItem(capsule = new CapsuleItem("capsule"), "capsule");
		ModelBakery.addVariantName(capsule, modid+":capsule_empty", modid+":capsule_activated", modid+":capsule_linked", modid+":capsule_deployed", modid+":capsule_broken");
    }
	
	public static void registerRenderers(String modid) {
		Minecraft.getMinecraft().getRenderItem().getItemModelMesher().register(CapsuleItemsRegistrer.capsule, 0, new ModelResourceLocation(modid+":capsule_empty", "inventory"));
		Minecraft.getMinecraft().getRenderItem().getItemModelMesher().register(CapsuleItemsRegistrer.capsule, 1, new ModelResourceLocation(modid+":capsule_activated", "inventory"));
		Minecraft.getMinecraft().getRenderItem().getItemModelMesher().register(CapsuleItemsRegistrer.capsule, 2, new ModelResourceLocation(modid+":capsule_linked", "inventory"));
		Minecraft.getMinecraft().getRenderItem().getItemModelMesher().register(CapsuleItemsRegistrer.capsule, 3, new ModelResourceLocation(modid+":capsule_deployed", "inventory"));
		Minecraft.getMinecraft().getRenderItem().getItemModelMesher().register(CapsuleItemsRegistrer.capsule, 4, new ModelResourceLocation(modid+":capsule_broken", "inventory"));
	}
	
	public static void registerRecipes() {
		ItemStack ironCapsule = new ItemStack(CapsuleItemsRegistrer.capsule, 1, CapsuleItem.STATE_EMPTY);
		ironCapsule.setTagInfo("color", new NBTTagInt(0xCCCCCC));
		ironCapsule.setTagInfo("size", new NBTTagInt(3));
		//ironCapsule.addEnchantment(Enchantments.recallEnchant, 1);
		
		ItemStack goldCapsule = new ItemStack(CapsuleItemsRegistrer.capsule, 1, CapsuleItem.STATE_EMPTY);
		goldCapsule.setTagInfo("color", new NBTTagInt(0xFFD700));
		goldCapsule.setTagInfo("size", new NBTTagInt(5));
		//goldCapsule.addEnchantment(Enchantments.recallEnchant, 1);
		
		ItemStack diamondCapsule = new ItemStack(CapsuleItemsRegistrer.capsule, 1, CapsuleItem.STATE_EMPTY);
		diamondCapsule.setTagInfo("color", new NBTTagInt(0x00FFF2));
		diamondCapsule.setTagInfo("size", new NBTTagInt(7));
		//diamondCapsule.addEnchantment(Enchantments.recallEnchant, 1);
		
		// base recipes
		GameRegistry.addRecipe(ironCapsule, new Object[] {"   ", "#P#", " # ", '#', Items.iron_ingot, 'P', Items.ender_pearl});
		GameRegistry.addRecipe(goldCapsule, new Object[] {"   ", "#P#", " # ", '#', Items.gold_ingot, 'P', Items.ender_pearl});
		GameRegistry.addRecipe(diamondCapsule, new Object[] {"   ", "#P#", " # ", '#', Items.diamond, 'P', Items.ender_pearl});
		
		// repair recipe
		GameRegistry.addRecipe(new RepairCapsuleRecipe(new ItemStack(CapsuleItemsRegistrer.capsule, 1, CapsuleItem.STATE_BROKEN), CapsuleItem.STATE_EMPTY));
		
		// dye recipe
		GameRegistry.addRecipe(new DyeCapsuleRecipe());
		
	}
}
