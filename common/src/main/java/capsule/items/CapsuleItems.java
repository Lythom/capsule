package capsule.items;

import capsule.CapsuleMod;
import capsule.helpers.NBTHelper;
import capsule.items.CapsuleItem.CapsuleState;
import capsule.platform.Services;
import capsule.recipes.BlueprintCapsuleRecipe;
import capsule.recipes.BlueprintChangeRecipe;
import capsule.recipes.PrefabsBlueprintAggregatorRecipe;
import capsule.recipes.RecoveryCapsuleRecipe;
import capsule.recipes.UpgradeCapsuleRecipe;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.IntTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.ShapedRecipe;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Supplier;

public class CapsuleItems {

    public static final int UPGRADE_STEP = 2;

    public static final Supplier<CapsuleItem> CAPSULE = Services.PLATFORM.register(BuiltInRegistries.ITEM, "capsule",
            () -> new CapsuleItem(new Item.Properties().setId(CapsuleMod.itemKey("capsule"))));

    public static ItemStack withState(CapsuleState state) {
        ItemStack capsule = new ItemStack(CapsuleItems.CAPSULE.get(), 1);
        CapsuleItem.setState(capsule, state);
        return capsule;
    }

    public static TreeMap<ItemStack, CraftingRecipe> capsuleList = new TreeMap<>(Comparator.comparingDouble(CapsuleItems::compare));
    public static TreeMap<ItemStack, CraftingRecipe> opCapsuleList = new TreeMap<>(Comparator.comparingDouble(CapsuleItems::compare));
    public static List<Pair<ItemStack, CraftingRecipe>> blueprintCapsules = new ArrayList<>();
    public static List<Pair<ItemStack, CraftingRecipe>> blueprintPrefabs = new ArrayList<>();
    public static Pair<ItemStack, CraftingRecipe> unlabelledCapsule = null;
    public static Pair<ItemStack, CraftingRecipe> deployedCapsule = null;
    public static Pair<ItemStack, RecoveryCapsuleRecipe> recoveryCapsule = null;
    public static Pair<ItemStack, BlueprintChangeRecipe> blueprintChangedCapsule = null;
    public static Pair<ItemStack, UpgradeCapsuleRecipe> upgradedCapsule = null;

    private static double compare(ItemStack capsule) {
        return CapsuleItem.getSize(capsule) + CapsuleItem.getMaterialColor(capsule) * 0.000000000001D;
    }

    /**
     * Fills the capsule lists of the creative tab and of the recipe viewers from the crafting recipes: on the client,
     * those the server sent (NeoForge RecipesReceivedEvent, Fabric ClientRecipeSynchronizedEvent).
     */
    public static void registerRecipesClient(Iterable<? extends RecipeHolder<?>> recipes) {
        blueprintCapsules.clear();
        blueprintPrefabs.clear();
        capsuleList.clear();
        opCapsuleList.clear();
        unlabelledCapsule = null;
        deployedCapsule = null;
        recoveryCapsule = null;
        blueprintChangedCapsule = null;
        upgradedCapsule = null;

        // create reference ItemStacks from json recipes
        // used for creative tab and JEI, disabled recipes should not raise here
        for (RecipeHolder<?> recipeHolder : recipes) {
            var recipe = recipeHolder.value();
            if (recipeHolder.id().identifier().getNamespace().equals("capsule") && hasNoEmptyTagsIngredient(recipe)) {
                if (recipe instanceof BlueprintCapsuleRecipe blueprint) {
                    blueprintCapsules.add(Pair.of(blueprint.getResultItem(), blueprint));
                } else if (recipe instanceof RecoveryCapsuleRecipe recovery) {
                    recoveryCapsule = Pair.of(recovery.getResultItem(), recovery);
                } else if (recipe instanceof UpgradeCapsuleRecipe upgrade) {
                    upgradedCapsule = Pair.of(upgrade.getResultItem(), upgrade);
                } else if (recipe instanceof BlueprintChangeRecipe blueprintChange) {
                    blueprintChangedCapsule = Pair.of(blueprintChange.getResultItem(), blueprintChange);
                } else if (recipe instanceof PrefabsBlueprintAggregatorRecipe agg) {
                    for (PrefabsBlueprintAggregatorRecipe.PrefabsBlueprintCapsuleRecipe aggregatorRecipe : agg.recipes) {
                        blueprintPrefabs.add(Pair.of(aggregatorRecipe.getResultItem(), aggregatorRecipe.recipe));
                    }
                } else if (recipe instanceof ShapedRecipe shaped) {
                    ItemStack output = shaped.assemble(CraftingInput.EMPTY);
                    if (output.getItem() instanceof CapsuleItem) {
                        if (CapsuleItem.isOverpowered(output)) {
                            CapsuleItems.opCapsuleList.put(output, shaped);
                        } else {
                            CapsuleItems.capsuleList.put(output, shaped);
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

    /**
     * False when an ingredient of the recipe matches no item, an empty tag for example (a material no mod adds).
     */
    public static boolean hasNoEmptyTagsIngredient(Recipe<?> recipe) {
        List<Ingredient> ingredients;
        if (recipe instanceof ShapedRecipe shaped) {
            ingredients = shaped.getIngredients().stream().flatMap(Optional::stream).toList();
        } else if (recipe instanceof RecoveryCapsuleRecipe recovery) {
            ingredients = recovery.ingredients;
        } else if (recipe instanceof UpgradeCapsuleRecipe upgrade) {
            ingredients = List.of(upgrade.upgradeIngredient);
        } else {
            ingredients = recipe.placementInfo().ingredients();
        }
        return ingredients.stream().noneMatch(Ingredient::isEmpty);
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
        NBTHelper.addTagElement(capsuleUp, "upgraded", IntTag.valueOf(upLevel));
        return capsuleUp;
    }

    public static void init() {
    }
}
