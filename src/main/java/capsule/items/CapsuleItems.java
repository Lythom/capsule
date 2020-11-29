package capsule.items;

import capsule.CapsuleMod;
import capsule.items.CapsuleItem.CapsuleState;
import capsule.recipes.*;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.ICraftingRecipe;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.RecipeManager;
import net.minecraft.item.crafting.ShapedRecipe;
import net.minecraft.nbt.IntNBT;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.RegistryEvent;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.TreeMap;

public class CapsuleItems {

    private static final int UPGRADE_STEP = 2;

    public static CapsuleItem CAPSULE;

    public static ItemStack withState(CapsuleState state) {
        ItemStack capsule = new ItemStack(CapsuleItems.CAPSULE, 1);
        CapsuleItem.setState(capsule, state);
        return capsule;
    }

    public static TreeMap<ItemStack, ICraftingRecipe> capsuleList = new TreeMap<>(Comparator.comparingDouble(CapsuleItems::compare));
    public static TreeMap<ItemStack, ICraftingRecipe> opCapsuleList = new TreeMap<>(Comparator.comparingDouble(CapsuleItems::compare));
    public static List<Pair<ItemStack, ICraftingRecipe>> blueprintCapsules = new ArrayList<>();
    public static List<Pair<ItemStack, ICraftingRecipe>> blueprintPrefabs = new ArrayList<>();
    public static Pair<ItemStack, ICraftingRecipe> unlabelledCapsule = null;
    public static Pair<ItemStack, ICraftingRecipe> deployedCapsule = null;
    public static Pair<ItemStack, RecoveryCapsuleRecipe> recoveryCapsule = null;
    public static Pair<ItemStack, BlueprintChangeRecipe> blueprintChangedCapsule = null;
    public static Pair<ItemStack, UpgradeCapsuleRecipe> upgradedCapsule = null;

    private static double compare(ItemStack capsule) {
        return CapsuleItem.getSize(capsule) + CapsuleItem.getMaterialColor(capsule) * 0.000000000001D;
    }

    public static void registerItems(RegistryEvent.Register<Item> event) {
        CAPSULE = new CapsuleItem();
        CAPSULE.setRegistryName(new ResourceLocation(CapsuleMod.MODID, "capsule"));
        event.getRegistry().register(CAPSULE);
    }

    @OnlyIn(Dist.CLIENT)
    public static void registerRecipesClient(RecipeManager manager) {
        blueprintCapsules.clear();
        blueprintPrefabs.clear();

        // create reference ItemStacks from json recipes
        // used for creative tab and JEI, disabled recipes should not raise here
        for (IRecipe<?> recipe : manager.getRecipes()) {
            if (recipe.getId().getNamespace().equals("capsule")) {
                if (recipe instanceof BlueprintCapsuleRecipe) {
                    blueprintCapsules.add(Pair.of(((BlueprintCapsuleRecipe) recipe).getRecipeOutput(), ((BlueprintCapsuleRecipe) recipe)));
                } else if (recipe instanceof RecoveryCapsuleRecipe) {
                    recoveryCapsule = Pair.of(((RecoveryCapsuleRecipe) recipe).getRecipeOutput(), (RecoveryCapsuleRecipe) recipe);
                } else if (recipe instanceof UpgradeCapsuleRecipe) {
                    upgradedCapsule = Pair.of(recipe.getRecipeOutput(), (UpgradeCapsuleRecipe) recipe);
                } else if (recipe instanceof BlueprintChangeRecipe) {
                    blueprintChangedCapsule = Pair.of(recipe.getRecipeOutput(), (BlueprintChangeRecipe) recipe);
                } else if (recipe instanceof PrefabsBlueprintAggregatorRecipe) {
                    PrefabsBlueprintAggregatorRecipe agg = (PrefabsBlueprintAggregatorRecipe) recipe;
                    for (PrefabsBlueprintAggregatorRecipe.PrefabsBlueprintCapsuleRecipe aggregatorRecipe : agg.recipes) {
                        blueprintPrefabs.add(Pair.of(aggregatorRecipe.getRecipeOutput(), aggregatorRecipe.recipe));
                    }
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

        if (CapsuleItems.capsuleList.size() > 0) {
            unlabelledCapsule = Pair.of(getUnlabelledCapsule(CapsuleItems.capsuleList.firstKey()), null);
            deployedCapsule = Pair.of(getDeployedCapsule(CapsuleItems.capsuleList.firstKey()), null);
        }
    }

    public static ItemStack getUnlabelledCapsule(ItemStack capsule) {
        ItemStack unlabelledCapsule = capsule.copy();
        CapsuleItem.setState(unlabelledCapsule, CapsuleState.LINKED);
        CapsuleItem.setStructureName(unlabelledCapsule, "config/capsule/rewards/example");
        return unlabelledCapsule;
    }

    public static ItemStack getDeployedCapsule(ItemStack capsule) {
        ItemStack unlabelledCapsule = capsule.copy();
        CapsuleItem.setState(unlabelledCapsule, CapsuleState.DEPLOYED);
        CapsuleItem.setStructureName(unlabelledCapsule, "config/capsule/rewards/example");
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
