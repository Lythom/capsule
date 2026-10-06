package capsule.plugins;

import capsule.CapsuleMod;
import capsule.Config;
import capsule.blocks.CapsuleBlocks;
import capsule.helpers.NBTHelper;
import capsule.items.CapsuleItem;
import capsule.items.CapsuleItems;
import net.minecraft.client.Minecraft;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static capsule.items.CapsuleItem.CapsuleState.EMPTY;

/**
 * What the recipe viewers (JEI, REI, EMI) show about capsules, beyond the json recipes: the special and dynamic
 * crafting recipes, and information pages. Built from the recipes synced to the client.
 */
public class RecipeViewerContent {
    private static final Logger LOGGER = LogManager.getLogger(RecipeViewerContent.class);

    public record Information(List<ItemStack> stacks, Component text) {
    }

    /**
     * Tells apart the capsule stacks a viewer must list separately.
     */
    public static String subtype(ItemStack stack) {
        if (!(stack.getItem() instanceof CapsuleItem)) return "";
        Component label = CapsuleItem.getLabel(stack);
        return String.valueOf(CapsuleItem.getState(stack))
                + CapsuleItem.getMaterialColor(stack)
                + CapsuleItem.isOverpowered(stack)
                + CapsuleItem.isBlueprint(stack)
                + (label == null ? "" : label.getString());
    }

    public static boolean isComplete() {
        boolean complete = CapsuleItems.recoveryCapsule != null && CapsuleItems.unlabelledCapsule != null
                && CapsuleItems.deployedCapsule != null && CapsuleItems.blueprintChangedCapsule != null
                && CapsuleItems.upgradedCapsule != null;
        if (!complete) {
            LOGGER.error("Some required capsule recipe is missing (recovery, regular capsules, upgrade or blueprintChanged recipe). The datapack might be corrupted for capsule or the recipe have been removed. Recipe viewers won't display the capsules and capsule might break at some points.");
        }
        return complete;
    }

    public static List<RecipeHolder<CraftingRecipe>> craftingRecipes() {
        List<RecipeHolder<CraftingRecipe>> recipes = new ArrayList<>();
        Ingredient upgradeIngredient = CapsuleItems.upgradedCapsule.getValue().upgradeIngredient;
        for (ItemStack capsule : CapsuleItems.capsuleList.keySet()) {
            for (int upLevel = 1; upLevel < Math.min(8, Config.upgradeLimit); upLevel++) {
                NonNullList<Ingredient> ingredients = NonNullList.withSize(upLevel + 1, upgradeIngredient);
                ingredients.set(0, Ingredient.of(capsule));
                add(recipes, "upgrade", CapsuleItems.getUpgradedCapsule(capsule, upLevel), ingredients);
            }
            add(recipes, "clear", capsule, NonNullList.of(Ingredient.EMPTY, Ingredient.of(CapsuleItems.getUnlabelledCapsule(capsule))));
        }
        recipes.add(new RecipeHolder<>(id("recovery", recipes), plain(CapsuleItems.recoveryCapsule.getValue())));
        for (Pair<ItemStack, CraftingRecipe> blueprint : CapsuleItems.blueprintCapsules) {
            recipes.add(new RecipeHolder<>(id("blueprint", recipes), plain(blueprint.getValue())));
        }
        for (Pair<ItemStack, CraftingRecipe> prefab : CapsuleItems.blueprintPrefabs) {
            recipes.add(new RecipeHolder<>(id("prefab", recipes), plain(prefab.getValue())));
        }
        Ingredient anyBlueprint = Ingredient.of(CapsuleItems.blueprintCapsules.stream().map(Pair::getKey).toArray(ItemStack[]::new));
        Ingredient anyCapsule = Ingredient.of(Stream.of(Stream.of(CapsuleItems.unlabelledCapsule.getKey()),
                Arrays.stream(anyBlueprint.getItems()), Stream.of(CapsuleItems.recoveryCapsule.getKey())).flatMap(s -> s).toArray(ItemStack[]::new));
        add(recipes, "blueprint_change", blueprintWithNewTemplate(), NonNullList.of(Ingredient.EMPTY, anyBlueprint, anyCapsule));
        return recipes;
    }

    public static List<Information> informationPages() {
        List<Information> pages = new ArrayList<>();
        pages.add(info(new ArrayList<>(CapsuleItems.capsuleList.keySet()), "capsule"));
        pages.add(info(List.of(blueprintWithNewTemplate()), "blueprintCapsule"));
        pages.add(info(List.of(CapsuleItems.unlabelledCapsule.getKey()), "linkedCapsule"));
        pages.add(info(List.of(CapsuleItems.deployedCapsule.getKey()), "linkedCapsule"));
        pages.add(info(List.of(CapsuleItems.recoveryCapsule.getKey()), "recoveryCapsule"));
        Stream.concat(CapsuleItems.blueprintCapsules.stream(), CapsuleItems.blueprintPrefabs.stream())
                .forEach(blueprint -> pages.add(info(List.of(blueprint.getKey()), "blueprintCapsule")));
        ItemStack opCapsule = CapsuleItems.withState(EMPTY);
        NBTHelper.updateTag(opCapsule, tag -> tag.putBoolean("overpowered", true));
        pages.add(info(List.of(opCapsule), "opCapsule"));
        pages.add(info(List.of(new ItemStack(CapsuleBlocks.CAPSULE_MARKER.get())), "capsuleMarker"));
        return pages;
    }

    private static ItemStack blueprintWithNewTemplate() {
        ItemStack stack = CapsuleItems.blueprintChangedCapsule.getKey().copy();
        CapsuleItem.setStructureName(stack, "newTemplate");
        CapsuleItem.setLabel(stack, "Changed Template");
        return stack;
    }

    /**
     * The recipe as a plain shaped or shapeless one: viewers skip special recipes.
     */
    private static CraftingRecipe plain(CraftingRecipe recipe) {
        ItemStack result = recipe.getResultItem(Minecraft.getInstance().level.registryAccess());
        if (recipe instanceof ShapedRecipe shaped) {
            return new ShapedRecipe(shaped.getGroup(), shaped.category(),
                    new ShapedRecipePattern(shaped.getWidth(), shaped.getHeight(), shaped.getIngredients(), Optional.empty()), result);
        }
        return new ShapelessRecipe(recipe.getGroup(), recipe.category(), result, recipe.getIngredients());
    }

    private static void add(List<RecipeHolder<CraftingRecipe>> recipes, String kind, ItemStack result, NonNullList<Ingredient> ingredients) {
        recipes.add(new RecipeHolder<>(id(kind, recipes), new ShapelessRecipe(CapsuleMod.MODID, CraftingBookCategory.MISC, result, ingredients)));
    }

    /**
     * Unique ids, marked synthetic with a leading slash as EMI expects.
     */
    private static ResourceLocation id(String kind, List<?> recipes) {
        return ResourceLocation.fromNamespaceAndPath(CapsuleMod.MODID, "/" + kind + "/" + recipes.size());
    }

    private static Information info(List<ItemStack> stacks, String key) {
        return new Information(stacks, Component.translatable("jei.capsule.desc." + key));
    }
}
