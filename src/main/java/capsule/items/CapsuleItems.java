package capsule.items;

import capsule.Main;
import capsule.recipes.BlueprintChangeRecipeFactory.BlueprintChangeRecipe;
import capsule.recipes.RecoveryBlueprintCapsuleRecipeFactory.RecoveryBlueprintCapsuleRecipe;
import capsule.recipes.UpgradeCapsuleRecipeFactory.UpgradeCapsuleRecipe;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.nbt.NBTTagInt;
import net.minecraftforge.event.RegistryEvent;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Comparator;
import java.util.TreeMap;

public class CapsuleItems {

    private static final int UPGRADE_STEP = 2;
    public static CapsuleItem capsule;

    public static String CAPSULE_REGISTERY_NAME = "capsule";

    public static TreeMap<ItemStack, IRecipe> capsuleList = new TreeMap<>(Comparator.comparingInt(CapsuleItem::getSize));
    public static TreeMap<ItemStack, IRecipe> opCapsuleList = new TreeMap<>(Comparator.comparingInt(CapsuleItem::getSize));
    public static Pair<ItemStack, IRecipe> unlabelledCapsule = null;
    public static Pair<ItemStack, RecoveryBlueprintCapsuleRecipe> recoveryCapsule = null;
    public static Pair<ItemStack, RecoveryBlueprintCapsuleRecipe> blueprintCapsule = null;
    public static Pair<ItemStack, BlueprintChangeRecipe> blueprintChangedCapsule = null;
    public static Pair<ItemStack, UpgradeCapsuleRecipe> upgradedCapsule = null;

    public static void registerItems(RegistryEvent.Register<Item> event) {
        capsule = new CapsuleItem(CAPSULE_REGISTERY_NAME);
        capsule.setCreativeTab(Main.tabCapsule);

        event.getRegistry().register(capsule.setRegistryName(CAPSULE_REGISTERY_NAME));
    }

    public static void registerRecipes(RegistryEvent.Register<IRecipe> event) {

        // create reference ItemStacks from json recipes
        // used for creative tab and JEI, disabled recipes should not raise here
        for (IRecipe recipe : event.getRegistry().getValuesCollection()) {
            if (recipe.getRegistryName().toString().startsWith("capsule:")) {

                if (recipe instanceof RecoveryBlueprintCapsuleRecipe) {
                    ItemStack out = recipe.getRecipeOutput();
                    if (CapsuleItem.isBlueprint(out)) {
                        blueprintCapsule = Pair.of(recipe.getRecipeOutput(), (RecoveryBlueprintCapsuleRecipe) recipe);
                    } else if (CapsuleItem.isOneUse(out)) {
                        recoveryCapsule = Pair.of(recipe.getRecipeOutput(), (RecoveryBlueprintCapsuleRecipe) recipe);
                    }
                } else if (recipe instanceof UpgradeCapsuleRecipe) {
                    upgradedCapsule = Pair.of(recipe.getRecipeOutput(), (UpgradeCapsuleRecipe) recipe);
                } else if (recipe instanceof BlueprintChangeRecipe) {
                    blueprintChangedCapsule = Pair.of(recipe.getRecipeOutput(), (BlueprintChangeRecipe) recipe);
                } else {
                    ItemStack output = recipe.getRecipeOutput();
                    if (output.getItem() instanceof CapsuleItem) {
                        if (CapsuleItem.isOverpowered(output)) {
                            CapsuleItems.opCapsuleList.put(output, recipe);
                        } else {
                            CapsuleItems.capsuleList.put(output, recipe);
                        }
                    }
                }
            }
        }

        if (CapsuleItems.capsuleList.size() > 0)
            unlabelledCapsule = Pair.of(getUnlabelledCapsule(CapsuleItems.capsuleList.firstKey()), null);
    }

    public static ItemStack getUnlabelledCapsule(ItemStack capsule) {
        ItemStack unlabelledCapsule = capsule.copy();
        unlabelledCapsule.setItemDamage(CapsuleItem.STATE_LINKED);
        CapsuleItem.setStructureName(unlabelledCapsule, "StructureNameExample");
        return unlabelledCapsule;
    }

    public static ItemStack getBlueprintCapsule(ItemStack capsule) {
        ItemStack unlabelledCapsule = capsule.copy();
        unlabelledCapsule.setItemDamage(CapsuleItem.STATE_LINKED);
        CapsuleItem.setStructureName(unlabelledCapsule, "StructureNameBlueprintExample");
        CapsuleItem.setBlueprint(unlabelledCapsule);
        return unlabelledCapsule;
    }

    public static ItemStack getRecoveryCapsule(ItemStack capsule) {
        ItemStack recoveryCapsule = capsule.copy();
        CapsuleItem.setOneUse(recoveryCapsule);
        CapsuleItem.setStructureName(recoveryCapsule, "StructureNameRecoveryExample");
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
