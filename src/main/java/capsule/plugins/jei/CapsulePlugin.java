package capsule.plugins.jei;

import capsule.Config;
import capsule.blocks.CapsuleBlocks;
import capsule.items.CapsuleItem;
import capsule.items.CapsuleItems;
import capsule.recipes.BlueprintCapsuleRecipeFactory.BlueprintCapsuleRecipe;
import capsule.recipes.PrefabsBlueprintCapsuleRecipe;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.IModRegistry;
import mezz.jei.api.ISubtypeRegistry;
import mezz.jei.api.JEIPlugin;
import mezz.jei.api.ingredients.VanillaTypes;
import mezz.jei.api.recipe.VanillaRecipeCategoryUid;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.item.crafting.ShapelessRecipe;
import net.minecraft.util.NonNullList;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;


@JEIPlugin
public class CapsulePlugin implements IModPlugin {

    @Override
    public void registerItemSubtypes(ISubtypeRegistry subtypeRegistry) {
        subtypeRegistry.registerSubtypeInterpreter(CapsuleItems.capsule, new CapsuleSubtypeInterpreter());
    }

    @Override
    public void register(@Nonnull IModRegistry registry) {

        // normally you should ignore nbt per-item, but these tags are universally understood
        // and apply to many vanilla and modded items
        List<IRecipe> recipes = new ArrayList<>();

        Ingredient upgradeIngredient = CapsuleItems.upgradedCapsule.getValue().upgradeIngredient;
        for (ItemStack capsule : CapsuleItems.capsuleList.keySet()) {
            for (int upLevel = 1; upLevel < Math.min(8, Config.upgradeLimit.get()); upLevel++) {
                ItemStack capsuleUp = CapsuleItems.getUpgradedCapsule(capsule, upLevel);
                NonNullList<Ingredient> ingredients = NonNullList.withSize(upLevel + 1, upgradeIngredient);
                ingredients.set(0, Ingredient.fromStacks(capsule));
                recipes.add(new ShapelessRecipes("capsule", capsuleUp, ingredients));
            }
            // clear
            recipes.add(new ShapelessRecipes("capsule", capsule, NonNullList.from(Ingredient.EMPTY, Ingredient.fromStacks(CapsuleItems.getUnlabelledCapsule(capsule)))));
        }

        ItemStack recoveryCapsule = CapsuleItems.recoveryCapsule.getKey();
        ItemStack unlabelled = CapsuleItems.unlabelledCapsule.getKey();
        ItemStack unlabelledDeployed = unlabelled.copy();
        CapsuleItem.setState(unlabelledDeployed, CapsuleItem.STATE_DEPLOYED);
        List<ItemStack> blueprintCapsules = CapsuleItems.blueprintCapsules.stream().map(Pair::getKey).collect(Collectors.toList());
        blueprintCapsules.add(CapsuleItems.blueprintChangedCapsule.getKey());
        Ingredient anyBlueprint = Ingredient.fromStacks(blueprintCapsules.toArray(new ItemStack[0]));
        Ingredient unlabelledIng = Ingredient.merge(Arrays.asList(Ingredient.fromStacks(unlabelled), anyBlueprint, Ingredient.fromStacks(recoveryCapsule)));
        // recovery
        recipes.add(CapsuleItems.recoveryCapsule.getValue().recipe);

        // blueprint
        for (Pair<ItemStack, IRecipe> r : CapsuleItems.blueprintCapsules) {
            if (r.getValue() instanceof BlueprintCapsuleRecipe) {
                recipes.add(((BlueprintCapsuleRecipe) r.getValue()).recipe);
            } else if (r.getValue() instanceof PrefabsBlueprintCapsuleRecipe) {
                recipes.add(((PrefabsBlueprintCapsuleRecipe) r.getValue()).recipe);
            } else {
                recipes.add(r.getValue());
            }
        }
        ItemStack withNewTemplate = CapsuleItems.blueprintChangedCapsule.getKey();
        CapsuleItem.setStructureName(withNewTemplate, "newTemplate");
        CapsuleItem.setLabel(withNewTemplate, "Changed Template");
        recipes.add(new ShapelessRecipes("capsule", withNewTemplate, NonNullList.from(Ingredient.EMPTY, anyBlueprint, unlabelledIng)));

        registry.addRecipes(recipes, VanillaRecipeCategoryUid.CRAFTING);
        registry.addIngredientInfo(new ArrayList<>(CapsuleItems.capsuleList.keySet()), VanillaTypes.ITEM, "jei.capsule.desc.capsule");
        registry.addIngredientInfo(unlabelled, VanillaTypes.ITEM, "jei.capsule.desc.linkedCapsule");
        registry.addIngredientInfo(unlabelledDeployed, VanillaTypes.ITEM, "jei.capsule.desc.linkedCapsule");
        registry.addIngredientInfo(recoveryCapsule, VanillaTypes.ITEM, "jei.capsule.desc.recoveryCapsule");
        registry.addIngredientInfo(Arrays.asList(anyBlueprint.getMatchingStacks()), VanillaTypes.ITEM, "jei.capsule.desc.blueprintCapsule");
        registry.addIngredientInfo(new ArrayList<>(CapsuleItems.opCapsuleList.keySet()), VanillaTypes.ITEM, "jei.capsule.desc.opCapsule");
        registry.addIngredientInfo(new ItemStack(CapsuleBlocks.blockCapsuleMarker), VanillaTypes.ITEM, "jei.capsule.desc.capsuleMarker");
    }


    private static class CapsuleSubtypeInterpreter implements ISubtypeRegistry.ISubtypeInterpreter {
        @Override
        public String apply(ItemStack itemStack) {
            if (!(itemStack.getItem() instanceof CapsuleItem)) return null;
            String isOP = String.valueOf(itemStack.getTag() != null && itemStack.getTag().contains("overpowered") && itemStack.getTag().getBoolean("overpowered"));
            String capsuleState = String.valueOf(itemStack.getDamage());
            String capsuleColor = String.valueOf(CapsuleItem.getMaterialColor(itemStack));
            String capsuleBlueprint = String.valueOf(CapsuleItem.isBlueprint(itemStack));
            String label = CapsuleItem.getLabel(itemStack);
            return capsuleState + capsuleColor + isOP + capsuleBlueprint + label;
        }
    }
}
