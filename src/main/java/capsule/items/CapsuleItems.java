package capsule.items;

import capsule.Main;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.nbt.NBTTagInt;
import net.minecraftforge.event.RegistryEvent;

import java.util.ArrayList;
import java.util.List;

public class CapsuleItems {

    private static final int UPGRADE_STEP = 2;
    public static CapsuleItem capsule;

    public static String CAPSULE_REGISTERY_NAME = "capsule";

    public static List<ItemStack> capsuleList = new ArrayList<>();
    public static List<ItemStack> opCapsuleList = new ArrayList<>();
    public static ItemStack unlabelledCapsule = null;
    public static ItemStack recoveryCapsule = null;

    public static void registerItems(RegistryEvent.Register<Item> event) {
        capsule = new CapsuleItem(CAPSULE_REGISTERY_NAME);
        capsule.setCreativeTab(Main.tabCapsule);

        event.getRegistry().register(capsule.setRegistryName(CAPSULE_REGISTERY_NAME));
    }

    public static void registerRecipes(RegistryEvent.Register<IRecipe> event) {

        // create reference ItemStacks from json recipes
        // used for creative tab and JEI, should not item if recipes are disabled
        for (IRecipe recipe : event.getRegistry().getValuesCollection()) {
            if (recipe.getRegistryName().toString().startsWith("capsule:")) {
                ItemStack output = recipe.getRecipeOutput();
                if (output.getItem() instanceof CapsuleItem) {
                    if (CapsuleItem.isOverpowered(output)) {
                        CapsuleItems.opCapsuleList.add(output);
                    } else {
                        CapsuleItems.capsuleList.add(output);
                    }
                }
            }
        }

        if (CapsuleItems.capsuleList.size() > 0) recoveryCapsule = getRecoveryCapsule(CapsuleItems.capsuleList.get(0));
        if (CapsuleItems.capsuleList.size() > 0) unlabelledCapsule = getUnlabelledCapsule(CapsuleItems.capsuleList.get(0));
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
