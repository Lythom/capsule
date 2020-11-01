package capsule.items;

import capsule.recipes.*;
import net.minecraft.client.Minecraft;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.ICraftingRecipe;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.ShapedRecipe;
import net.minecraft.nbt.IntNBT;
import net.minecraftforge.event.RegistryEvent;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.TreeMap;

public class CapsuleItems {

    private static final int UPGRADE_STEP = 2;
    public static CapsuleItem capsule;

    public static ItemStack withState(int state) {
        ItemStack capsule = new ItemStack(CapsuleItems.capsule, 1);
        CapsuleItem.setState(capsule, state);
        return capsule;
    }

    public static String CAPSULE_REGISTERY_NAME = "capsule";

    public static TreeMap<ItemStack, ICraftingRecipe> capsuleList = new TreeMap<>(Comparator.comparingDouble(CapsuleItems::compare));
    public static TreeMap<ItemStack, ICraftingRecipe> opCapsuleList = new TreeMap<>(Comparator.comparingDouble(CapsuleItems::compare));
    public static List<Pair<ItemStack, ICraftingRecipe>> blueprintCapsules = new ArrayList<>();
    public static Pair<ItemStack, ICraftingRecipe> unlabelledCapsule = null;
    public static Pair<ItemStack, RecoveryCapsuleRecipe> recoveryCapsule = null;
    public static Pair<ItemStack, BlueprintChangeRecipe> blueprintChangedCapsule = null;
    public static Pair<ItemStack, UpgradeCapsuleRecipe> upgradedCapsule = null;

    private static double compare(ItemStack capsule) {
        return CapsuleItem.getSize(capsule) + CapsuleItem.getMaterialColor(capsule) * 0.000000000001D;
    }

    public static void registerItems(RegistryEvent.Register<Item> event) {
        capsule = new CapsuleItem();

        event.getRegistry().register(capsule.setRegistryName(CAPSULE_REGISTERY_NAME));
    }

    public static void registerRecipes() {
        // create reference ItemStacks from json recipes
        // used for creative tab and JEI, disabled recipes should not raise here
        for (IRecipe<?> recipe : Minecraft.getInstance().getIntegratedServer().getRecipeManager().getRecipes()) {
            if (recipe.toString().startsWith("capsule:")) {

                if (recipe instanceof BlueprintCapsuleRecipe) {
                    blueprintCapsules.add(Pair.of(((BlueprintCapsuleRecipe) recipe).getRecipeOutput(), (BlueprintCapsuleRecipe) recipe));
                } else if (recipe instanceof RecoveryCapsuleRecipe) {
                    recoveryCapsule = Pair.of(((RecoveryCapsuleRecipe) recipe).getRecipeOutput(), (RecoveryCapsuleRecipe) recipe);
                } else if (recipe instanceof UpgradeCapsuleRecipe) {
                    upgradedCapsule = Pair.of(recipe.getRecipeOutput(), (UpgradeCapsuleRecipe) recipe);
                } else if (recipe instanceof BlueprintChangeRecipe) {
                    blueprintChangedCapsule = Pair.of(recipe.getRecipeOutput(), (BlueprintChangeRecipe) recipe);
                } else if (recipe instanceof PrefabsBlueprintAggregatorRecipe.PrefabsBlueprintCapsuleRecipe) {
                    blueprintCapsules.add(Pair.of(recipe.getRecipeOutput(), (PrefabsBlueprintAggregatorRecipe.PrefabsBlueprintCapsuleRecipe) recipe));
                } else {
                    ItemStack output = recipe.getRecipeOutput();
                    if (output.getItem() instanceof CapsuleItem && recipe instanceof ShapedRecipe) {
                        if (CapsuleItem.isOverpowered(output)) {
                            CapsuleItems.opCapsuleList.put(output, (ShapedRecipe) recipe);
                        } else {
                            CapsuleItems.capsuleList.put(output, (ShapedRecipe) recipe);
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
        unlabelledCapsule.setDamage(CapsuleItem.STATE_LINKED);
        CapsuleItem.setStructureName(unlabelledCapsule, "StructureNameExample");
        return unlabelledCapsule;
    }

    public static ItemStack getUpgradedCapsule(ItemStack ironCapsule, int upLevel) {
        ItemStack capsuleUp = ironCapsule.copy();
        CapsuleItem.setSize(capsuleUp, CapsuleItem.getSize(ironCapsule) + upLevel * UPGRADE_STEP);
        CapsuleItem.setUpgradeLevel(capsuleUp, upLevel);
        capsuleUp.setTagInfo("upgraded", IntNBT.valueOf(upLevel));
        return capsuleUp;
    }

}
