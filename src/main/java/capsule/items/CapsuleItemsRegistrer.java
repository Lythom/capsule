package capsule.items;

import static net.minecraftforge.oredict.RecipeSorter.Category.SHAPELESS;

import capsule.Config;
import capsule.Main;
import capsule.items.recipes.ClearCapsuleRecipe;
import capsule.items.recipes.DyeCapsuleRecipe;
import capsule.items.recipes.RecoveryCapsuleRecipe;
import capsule.items.recipes.UpgradeCapsuleRecipe;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagByte;
import net.minecraft.nbt.NBTTagInt;
import net.minecraftforge.common.config.Property;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.oredict.RecipeSorter;

public class CapsuleItemsRegistrer {

	public static Item capsule;
	public static Item recoveryCapsule;
	public static Item creativeTP;
	
	public static String CAPSULETP_REGISTERY_NAME = "capsule_CTP";
	public static String CAPSULE_REGISTERY_NAME = "capsule";
	
	public static UpgradeCapsuleRecipe upgradeCapsuleRecipe;
	public static RecoveryCapsuleRecipe recoveryCapsuleRecipe;
	public static DyeCapsuleRecipe dyeCapsuleRecipe;
	public static ClearCapsuleRecipe clearCapsuleRecipe;
	public static Property ironCapsuleSize;
	public static Property goldCapsuleSize;
	public static Property diamondCapsuleSize;
	public static Property opCapsuleSize;

	public static void createItems(String modid) {
		creativeTP = new CreativeTP(CAPSULETP_REGISTERY_NAME);
		creativeTP.setCreativeTab(Main.tabCapsule);
		
		capsule = new CapsuleItem(CAPSULE_REGISTERY_NAME);
		capsule.setCreativeTab(Main.tabCapsule);

		GameRegistry.register(creativeTP.setRegistryName(CAPSULETP_REGISTERY_NAME));
		GameRegistry.register(capsule.setRegistryName(CAPSULE_REGISTERY_NAME));
	}

	public static void registerRecipes() {

		ironCapsuleSize = Config.config.get("Balancing", "ironCapsuleSize", "1");
		ironCapsuleSize.setComment("Size of the capture cube side for an Iron Capsule. Must be an Odd Number (or it will be rounded down with error message).\n0 to disable.\nDefault: 1");

		goldCapsuleSize = Config.config.get("Balancing", "goldCapsuleSize", "3");
		goldCapsuleSize.setComment("Size of the capture cube side for a Gold Capsule. Must be an Odd Number (or it will be rounded down with error message).\n0 to disable.\nDefault: 3");

		diamondCapsuleSize = Config.config.get("Balancing", "diamondCapsuleSize", "5");
		diamondCapsuleSize.setComment("Size of the capture cube side for a Diamond Capsule. Must be an Odd Number (or it will be rounded down with error message).\n0 to disable.\nDefault: 5");

		opCapsuleSize = Config.config.get("Balancing", "opCapsuleSize", "1");
		opCapsuleSize.setComment("Size of the capture cube side for a Overpowered Capsule. Must be an Odd Number (or it will be rounded down with error message).\n0 to disable.\nDefault: 1");

		Property upgradesLimit = Config.config.get("Balancing", "capsuleUpgradesLimit", 5);
		upgradesLimit.setComment("Number of upgrades an empty capsules can get to improve capacity. If <= 0, remove the upgrade recipe.");

		Config.config.save();

		ItemStack ironCapsule = createCapsuleItemStack(0xCCCCCC, ironCapsuleSize.getInt());
		ItemStack goldCapsule = createCapsuleItemStack(0xFFD700, goldCapsuleSize.getInt());
		ItemStack diamondCapsule = createCapsuleItemStack(0x00FFF2, diamondCapsuleSize.getInt());
		ItemStack opCapsule = createCapsuleItemStack(0xFFFFFF, opCapsuleSize.getInt());
		opCapsule.setTagInfo("overpowered", new NBTTagByte((byte) 1));

		// base recipes
		GameRegistry.addRecipe(ironCapsule, new Object[] { " B ", "#P#", " # ", '#', Items.IRON_INGOT, 'P', Items.ENDER_PEARL, 'B', Blocks.STONE_BUTTON });
		GameRegistry.addRecipe(goldCapsule, new Object[] { " B ", "#P#", " # ", '#', Items.GOLD_INGOT, 'P', Items.ENDER_PEARL, 'B', Blocks.STONE_BUTTON });
		GameRegistry.addRecipe(diamondCapsule, new Object[] { " B ", "#P#", " # ", '#', Items.DIAMOND, 'P', Items.ENDER_PEARL, 'B', Blocks.STONE_BUTTON });
		GameRegistry.addRecipe(opCapsule, new Object[] { " B ", "#N#", " # ", '#', Items.DIAMOND, 'N', Items.NETHER_STAR, 'B', Blocks.STONE_BUTTON });

		// capsule upgrade recipe
		int upgradesMaxCount = upgradesLimit.getInt();
		RecipeSorter.register(CAPSULE_REGISTERY_NAME+":upgrade", UpgradeCapsuleRecipe.class, SHAPELESS, "after:minecraft:shapeless");
		CapsuleItemsRegistrer.upgradeCapsuleRecipe = new UpgradeCapsuleRecipe(Items.ENDER_PEARL, upgradesMaxCount);
		GameRegistry.addRecipe(CapsuleItemsRegistrer.upgradeCapsuleRecipe);		


		// recovery capsule recipe
		RecipeSorter.register(CAPSULE_REGISTERY_NAME+":recovery", RecoveryCapsuleRecipe.class, SHAPELESS, "after:minecraft:shapeless");
		CapsuleItemsRegistrer.recoveryCapsuleRecipe = new RecoveryCapsuleRecipe(new ItemStack(CapsuleItemsRegistrer.capsule, 1, CapsuleItem.STATE_LINKED),
				new ItemStack(Items.GLASS_BOTTLE), CapsuleItem.STATE_ONE_USE);
		GameRegistry.addRecipe(CapsuleItemsRegistrer.recoveryCapsuleRecipe);
		
		RecipeSorter.register(CAPSULE_REGISTERY_NAME+":clear", RecoveryCapsuleRecipe.class, SHAPELESS, "after:minecraft:shapeless");
		CapsuleItemsRegistrer.clearCapsuleRecipe = new ClearCapsuleRecipe();
		GameRegistry.addRecipe(CapsuleItemsRegistrer.clearCapsuleRecipe);

		// dye recipe
		RecipeSorter.register(CAPSULE_REGISTERY_NAME+":dye", DyeCapsuleRecipe.class, SHAPELESS, "after:minecraft:shapeless");
		CapsuleItemsRegistrer.dyeCapsuleRecipe = new DyeCapsuleRecipe();
		GameRegistry.addRecipe(CapsuleItemsRegistrer.dyeCapsuleRecipe);

	}

	/**
	 * Create a Stack or return null if size <= 0
	 * 
	 * @param color
	 * @param size
	 * @return
	 */
	public static ItemStack createCapsuleItemStack(int color, int size) {
		if (size <= 0)
			return null;
		ItemStack stack = new ItemStack(CapsuleItemsRegistrer.capsule, 1, CapsuleItem.STATE_EMPTY);
		stack.setTagInfo("color", new NBTTagInt(color));
		stack.setTagInfo("size", new NBTTagInt(size));
		return stack;
	}

}
