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
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.nbt.NBTTagByte;
import net.minecraft.nbt.NBTTagInt;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;

import java.util.ArrayList;
import java.util.List;

public class CapsuleItems {

    private static final int UPGRADE_STEP = 2;
    public static CapsuleItem capsule;

    public static String CAPSULE_REGISTERY_NAME = "capsule";

    public static UpgradeCapsuleRecipe upgradeCapsuleRecipe;
    public static RecoveryCapsuleRecipe recoveryCapsuleRecipe;
    public static ClearCapsuleRecipe clearCapsuleRecipe;
    public static DyeCapsuleRecipe dyeCapsuleRecipe;

    public static List<ItemStack> capsules = new ArrayList<>();
    public static List<ItemStack> opCapsules = new ArrayList<>();

    public static void registerItems(RegistryEvent.Register<Item> event) {
        capsule = new CapsuleItem(CAPSULE_REGISTERY_NAME);
        capsule.setCreativeTab(Main.tabCapsule);

        event.getRegistry().register(capsule.setRegistryName(CAPSULE_REGISTERY_NAME));

        // create reference ItemStacks
//        capsules.add(CapsuleItems.createCapsuleItemStack(0xCCCCCC, Config.ironCapsuleSize.get()));
//        capsules.add(CapsuleItems.createCapsuleItemStack(0xFFD700, Config.goldCapsuleSize.get()));
//        capsules.add(CapsuleItems.createCapsuleItemStack(0x00FFF2, Config.diamondCapsuleSize.get()));
//        ItemStack opCapsule = CapsuleItems.createCapsuleItemStack(0xFFFFFF, Config.opCapsuleSize.get());
//        opCapsule.setTagInfo("overpowered", new NBTTagByte((byte) 1));
//        opCapsules.add(opCapsule);
    }

    public static void registerRecipes(RegistryEvent.Register<IRecipe> event) {

        // create reference ItemStacks from json recipes
        // used for creative tab and JEI, should not item if recipes are disabled
        for (IRecipe recipe : event.getRegistry().getValuesCollection()) {
            if (recipe.getRegistryName().toString().startsWith("capsule:")) {
                ItemStack output = recipe.getRecipeOutput();
                if (output.getItem() instanceof CapsuleItem) {
                    if (capsule.isOverpowered(output)) {
                        opCapsules.add(output);
                    } else {
                        opCapsules.add(output);
                    }
                }
            }
        }

        // capsule upgrade recipe
        upgradeCapsuleRecipe = new UpgradeCapsuleRecipe(Items.CHORUS_FRUIT_POPPED);
        upgradeCapsuleRecipe.setRegistryName(CAPSULE_REGISTERY_NAME + ":upgrade");
        event.getRegistry().register(upgradeCapsuleRecipe);

        // capsule recovery recipe
        recoveryCapsuleRecipe = new RecoveryCapsuleRecipe(
                new ItemStack(CapsuleItems.capsule, 1, CapsuleItem.STATE_LINKED),
                new ItemStack(Items.GLASS_BOTTLE),
                CapsuleItem.STATE_ONE_USE
        );
        recoveryCapsuleRecipe.setRegistryName(CAPSULE_REGISTERY_NAME + ":recovery");
        event.getRegistry().register(recoveryCapsuleRecipe);

        // capsule return to empty state recipe
        clearCapsuleRecipe = new ClearCapsuleRecipe();
        clearCapsuleRecipe.setRegistryName(CAPSULE_REGISTERY_NAME + ":clear");
        event.getRegistry().register(clearCapsuleRecipe);

        // capsule dye to empty state recipe
        dyeCapsuleRecipe = new DyeCapsuleRecipe();
        dyeCapsuleRecipe.setRegistryName(CAPSULE_REGISTERY_NAME + ":dye");
        event.getRegistry().register(dyeCapsuleRecipe);
    }

    public static void registerRecipes() {

        // 0 should disable recipe
        // config shoudl overwrite json recipe.
/*

        ItemStack ironCapsule = createCapsuleItemStack(0xCCCCCC, Config.ironCapsuleSize);
        ItemStack goldCapsule = createCapsuleItemStack(0xFFD700, Config.goldCapsuleSize);
        ItemStack diamondCapsule = createCapsuleItemStack(0x00FFF2, Config.diamondCapsuleSize);
        ItemStack opCapsule = createCapsuleItemStack(0xFFFFFF, Config.opCapsuleSize);
        if (opCapsule != null) opCapsule.setTagInfo("overpowered", new NBTTagByte((byte) 1));

        // base recipes
        GameRegistry.addShapedRecipe(ironCapsule, new Object[]{" B ", "#P#", " # ", '#', Items.IRON_INGOT, 'P', Items.ENDER_PEARL, 'B', Blocks.STONE_BUTTON});
        GameRegistry.addShapedRecipe(goldCapsule, new Object[]{" B ", "RPR", " # ", '#', Items.IRON_INGOT, 'R', Items.GOLD_INGOT, 'P', Items.ENDER_PEARL, 'B', Blocks.STONE_BUTTON});
        GameRegistry.addShapedRecipe(diamondCapsule, new Object[]{" B ", "RPR", " # ", '#', Items.IRON_INGOT, 'R', Items.DIAMOND, 'P', Items.ENDER_PEARL, 'B', Blocks.STONE_BUTTON});
        GameRegistry.addShapedRecipe(opCapsule, new Object[]{" B ", "#N#", " # ", '#', Items.IRON_INGOT, 'N', Items.NETHER_STAR, 'B', Blocks.STONE_BUTTON});
*/
    }

    /**
     * Create a Stack. Size will be 1 if size <= 0.
     *
     * @param color color of the capsule
     * @param size  size of the capsule
     * @return new empty capsule ItemStack
     */
    public static ItemStack createCapsuleItemStack(int color, int size) {
        int actualSize = 1;
        if (size > 0) actualSize = size;
        ItemStack stack = new ItemStack(CapsuleItems.capsule, 1, CapsuleItem.STATE_EMPTY);
        stack.setTagInfo("color", new NBTTagInt(color));
        stack.setTagInfo("size", new NBTTagInt(actualSize));
        return stack;
    }


    public static ItemStack getUnlabelledCapsule(ItemStack capsule) {
        ItemStack unlabelledCapsule = capsule.copy();
        unlabelledCapsule.setItemDamage(CapsuleItem.STATE_LINKED);
        CapsuleItem.setStructureName(unlabelledCapsule, "JEIExemple");
        return unlabelledCapsule;
    }

    public static ItemStack getRecoveryCapsule(ItemStack capsule) {
        ItemStack recoveryCapsule = capsule.copy();
        CapsuleItem.setOneUse(recoveryCapsule);
        CapsuleItem.setStructureName(recoveryCapsule, "JEIExemple");
        return recoveryCapsule;
    }

    public static ItemStack getUpgradedCapsule(ItemStack ironCapsule, int upLevel) {
        ItemStack capsuleUp = ironCapsule.copy();
        CapsuleItem.setSize(capsuleUp, CapsuleItem.getSize(ironCapsule) + upLevel * UPGRADE_STEP);
        CapsuleItem.setUpgradeLevel(capsuleUp, upLevel);
        capsuleUp.setTagInfo("upgraded", new NBTTagInt(upLevel));
        return capsuleUp;
    }

}
