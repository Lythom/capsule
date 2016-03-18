package capsule.items;

import capsule.Config;
import capsule.Main;
import capsule.items.recipes.DyeCapsuleRecipe;
import capsule.items.recipes.RecoveryCapsuleRecipe;
import capsule.items.recipes.UpgradeCapsuleRecipe;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagInt;
import net.minecraftforge.common.config.Property;
import net.minecraftforge.fml.common.registry.GameRegistry;

public class CapsuleItemsRegistrer {
	
	public static Item capsule;
	public static Item creativeTP;
	
	public static void createItems(String modid) {
		GameRegistry.registerItem(creativeTP = new CreativeTP("capsule_CTP"), "capsule_CTP");
		creativeTP.setCreativeTab(Main.tabCapsule);
		GameRegistry.registerItem(capsule = new CapsuleItem("capsule"), "capsule");
		capsule.setCreativeTab(Main.tabCapsule);
    }
	
	public static void registerRecipes() {
		
		Property ironCapsuleSize = Config.config.get("Balancing", "ironCapsuleSize", "3");
		ironCapsuleSize.comment = "Size of the capture cube side for an Iron Capsule. Must be an Odd Number (or it will be rounded down with error message).\n0 to disable.\nDefault: 3";
		
		Property goldCapsuleSize = Config.config.get("Balancing", "goldCapsuleSize", "5");
		goldCapsuleSize.comment = "Size of the capture cube side for a Gold Capsule. Must be an Odd Number (or it will be rounded down with error message).\n0 to disable.\nDefault: 5";
		
		Property diamondCapsuleSize = Config.config.get("Balancing", "diamondCapsuleSize", "7");
		diamondCapsuleSize.comment = "Size of the capture cube side for a Diamond Capsule. Must be an Odd Number (or it will be rounded down with error message).\n0 to disable.\nDefault: 7";

		Property upgradesLimit = Config.config.get("Balancing", "capsuleUpgradesLimit", 10);
		upgradesLimit.comment = "Number of upgrades an empty capsules can get to improve capacity. If <= 0, remove the upgrade recipe.";
		
		Config.config.save();
		
		ItemStack ironCapsule = createCapsuleItemStack(0xCCCCCC, ironCapsuleSize.getInt());
		ItemStack goldCapsule = createCapsuleItemStack(0xFFD700, goldCapsuleSize.getInt());
		ItemStack diamondCapsule = createCapsuleItemStack(0x00FFF2, diamondCapsuleSize.getInt());
		
		// base recipes
		GameRegistry.addRecipe(ironCapsule, new Object[] {" B ", "#P#", " # ", '#', Items.iron_ingot, 'P', Items.ender_pearl, 'B', Blocks.stone_button});
		GameRegistry.addRecipe(goldCapsule, new Object[] {" B ", "#P#", " # ", '#', Items.gold_ingot, 'P', Items.ender_pearl, 'B', Blocks.stone_button});
		GameRegistry.addRecipe(diamondCapsule, new Object[] {" B ", "#P#", " # ", '#', Items.diamond, 'P', Items.ender_pearl, 'B', Blocks.stone_button});
		
		// capsule upgrade recipe
		int upgradesMaxCount = upgradesLimit.getInt();
		if(upgradesMaxCount > 0){
			GameRegistry.addRecipe(new UpgradeCapsuleRecipe(Items.ender_pearl, upgradesMaxCount));
		}
		
		// recovery capsule recipe
		GameRegistry.addRecipe(new RecoveryCapsuleRecipe(new ItemStack(CapsuleItemsRegistrer.capsule, 1, CapsuleItem.STATE_LINKED), new ItemStack(Items.glass_bottle), CapsuleItem.STATE_ONE_USE));
		
		// dye recipe
		GameRegistry.addRecipe(new DyeCapsuleRecipe());
		
	}
	
	/**
	 * Create a Stack or return null if size <= 0
	 * @param color
	 * @param size
	 * @return
	 */
	public static ItemStack createCapsuleItemStack(int color, int size){
		if(size <= 0) return null;
		ItemStack stack = new ItemStack(CapsuleItemsRegistrer.capsule, 1, CapsuleItem.STATE_EMPTY);
		stack.setTagInfo("color", new NBTTagInt(color));
		stack.setTagInfo("size", new NBTTagInt(size));
		return stack;
	}
}
