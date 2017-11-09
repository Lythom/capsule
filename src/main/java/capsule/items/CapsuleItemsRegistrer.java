package capsule.items;

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
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.oredict.RecipeSorter;

import static net.minecraftforge.oredict.RecipeSorter.Category.SHAPELESS;

public class CapsuleItemsRegistrer {

    public static Item capsule;

    public static String CAPSULE_REGISTERY_NAME = "capsule";

    public static UpgradeCapsuleRecipe upgradeCapsuleRecipe;
    public static RecoveryCapsuleRecipe recoveryCapsuleRecipe;
    public static DyeCapsuleRecipe dyeCapsuleRecipe;
    public static ClearCapsuleRecipe clearCapsuleRecipe;

    public static void registerItems() {
        capsule = new CapsuleItem(CAPSULE_REGISTERY_NAME);
        capsule.setCreativeTab(Main.tabCapsule);

        GameRegistry.register(capsule.setRegistryName(CAPSULE_REGISTERY_NAME));
    }

    public static void registerRecipes() {

        ItemStack ironCapsule = createCapsuleItemStack(0xCCCCCC, Config.ironCapsuleSize);
        ItemStack goldCapsule = createCapsuleItemStack(0xFFD700, Config.goldCapsuleSize);
        ItemStack diamondCapsule = createCapsuleItemStack(0x00FFF2, Config.diamondCapsuleSize);
        ItemStack opCapsule = createCapsuleItemStack(0xFFFFFF, Config.opCapsuleSize);
        if (opCapsule != null) opCapsule.setTagInfo("overpowered", new NBTTagByte((byte) 1));

        // base recipes
        GameRegistry.addRecipe(ironCapsule, new Object[]{" B ", "#P#", " # ", '#', Items.IRON_INGOT, 'P', Items.ENDER_PEARL, 'B', Blocks.STONE_BUTTON});
        GameRegistry.addRecipe(goldCapsule, new Object[]{" B ", "RPR", " # ", '#', Items.IRON_INGOT, 'R', Items.GOLD_INGOT, 'P', Items.ENDER_PEARL, 'B', Blocks.STONE_BUTTON});
        GameRegistry.addRecipe(diamondCapsule, new Object[]{" B ", "RPR", " # ", '#', Items.IRON_INGOT, 'R', Items.DIAMOND, 'P', Items.ENDER_PEARL, 'B', Blocks.STONE_BUTTON});
        GameRegistry.addRecipe(opCapsule, new Object[]{" B ", "#N#", " # ", '#', Items.IRON_INGOT, 'N', Items.NETHER_STAR, 'B', Blocks.STONE_BUTTON});

        // capsule upgrade recipe
        RecipeSorter.register(CAPSULE_REGISTERY_NAME + ":upgrade", UpgradeCapsuleRecipe.class, SHAPELESS, "after:minecraft:shapeless");
        CapsuleItemsRegistrer.upgradeCapsuleRecipe = new UpgradeCapsuleRecipe(Items.CHORUS_FRUIT_POPPED);
        GameRegistry.addRecipe(CapsuleItemsRegistrer.upgradeCapsuleRecipe);

        // recovery capsule recipe
        RecipeSorter.register(CAPSULE_REGISTERY_NAME + ":recovery", RecoveryCapsuleRecipe.class, SHAPELESS, "after:minecraft:shapeless");
        CapsuleItemsRegistrer.recoveryCapsuleRecipe = new RecoveryCapsuleRecipe(new ItemStack(CapsuleItemsRegistrer.capsule, 1, CapsuleItem.STATE_LINKED),
                new ItemStack(Items.GLASS_BOTTLE), CapsuleItem.STATE_ONE_USE);
        GameRegistry.addRecipe(CapsuleItemsRegistrer.recoveryCapsuleRecipe);

        RecipeSorter.register(CAPSULE_REGISTERY_NAME + ":clear", RecoveryCapsuleRecipe.class, SHAPELESS, "after:minecraft:shapeless");
        CapsuleItemsRegistrer.clearCapsuleRecipe = new ClearCapsuleRecipe();
        GameRegistry.addRecipe(CapsuleItemsRegistrer.clearCapsuleRecipe);

        // dye recipe
        RecipeSorter.register(CAPSULE_REGISTERY_NAME + ":dye", DyeCapsuleRecipe.class, SHAPELESS, "after:minecraft:shapeless");
        CapsuleItemsRegistrer.dyeCapsuleRecipe = new DyeCapsuleRecipe();
        GameRegistry.addRecipe(CapsuleItemsRegistrer.dyeCapsuleRecipe);

    }

    /**
     * Create a Stack or return null if size <= 0
     *
     * @param color color of the capsule
     * @param size  size of the capsule
     * @return new empty capsule ItemStack
     */
    public static ItemStack createCapsuleItemStack(int color, int size) {
        int actualSize = 1;
        if (size > 0) actualSize = size;
        ItemStack stack = new ItemStack(CapsuleItemsRegistrer.capsule, 1, CapsuleItem.STATE_EMPTY);
        stack.setTagInfo("color", new NBTTagInt(color));
        stack.setTagInfo("size", new NBTTagInt(actualSize));
        return stack;
    }

}
