package capsule.items;

import capsule.CapsuleMod;
import capsule.helpers.NBTHelper;
import capsule.items.CapsuleItem.CapsuleState;
import capsule.recipes.BlueprintCapsuleRecipe;
import capsule.recipes.BlueprintChangeRecipe;
import capsule.recipes.PrefabsBlueprintAggregatorRecipe;
import capsule.recipes.RecoveryCapsuleRecipe;
import capsule.recipes.UpgradeCapsuleRecipe;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.IntTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.TreeMap;

public class CapsuleItems {

    private static final int UPGRADE_STEP = 2;

    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(CapsuleMod.MODID);
    public static final DeferredItem<CapsuleItem> CAPSULE = ITEMS.register("capsule", CapsuleItem::new);

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

    @OnlyIn(Dist.CLIENT)
    public static void registerRecipesClient(RecipeManager manager) {
        blueprintCapsules.clear();
        blueprintPrefabs.clear();
        capsuleList.clear();
        opCapsuleList.clear();
        unlabelledCapsule = null;
        deployedCapsule = null;
        recoveryCapsule = null;
        blueprintChangedCapsule = null;
        upgradedCapsule = null;

        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) {
            throw new NullPointerException("level must not be null.");
        }
        HolderLookup.Provider registryAccess = level.registryAccess();

        // create reference ItemStacks from json recipes
        // used for creative tab and JEI, disabled recipes should not raise here
        for (RecipeHolder<?> recipeHolder : manager.getRecipes()) {
            var recipe = recipeHolder.value();
            if (recipeHolder.id().getNamespace().equals("capsule") && hasNoEmptyTagsIngredient(recipe)) {
                if (recipe instanceof BlueprintCapsuleRecipe) {
                    blueprintCapsules.add(Pair.of(((BlueprintCapsuleRecipe) recipe).getResultItem(registryAccess), ((BlueprintCapsuleRecipe) recipe)));
                } else if (recipe instanceof RecoveryCapsuleRecipe) {
                    recoveryCapsule = Pair.of(((RecoveryCapsuleRecipe) recipe).getResultItem(registryAccess), (RecoveryCapsuleRecipe) recipe);
                } else if (recipe instanceof UpgradeCapsuleRecipe) {
                    upgradedCapsule = Pair.of(recipe.getResultItem(registryAccess), (UpgradeCapsuleRecipe) recipe);
                } else if (recipe instanceof BlueprintChangeRecipe) {
                    blueprintChangedCapsule = Pair.of(recipe.getResultItem(registryAccess), (BlueprintChangeRecipe) recipe);
                } else if (recipe instanceof PrefabsBlueprintAggregatorRecipe) {
                    PrefabsBlueprintAggregatorRecipe agg = (PrefabsBlueprintAggregatorRecipe) recipe;
                    for (PrefabsBlueprintAggregatorRecipe.PrefabsBlueprintCapsuleRecipe aggregatorRecipe : agg.recipes) {
                        blueprintPrefabs.add(Pair.of(aggregatorRecipe.getResultItem(registryAccess), aggregatorRecipe.recipe));
                    }
                } else {
                    ItemStack output = recipe.getResultItem(registryAccess);
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

    private static boolean hasNoEmptyTagsIngredient(Recipe<?> recipe) {
        return recipe.getIngredients().stream().allMatch(i -> i.isEmpty() || Arrays.stream(i.getItems()).noneMatch(s -> s.getItem() == Items.BARRIER));
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

    public static void registerItems(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }
}
