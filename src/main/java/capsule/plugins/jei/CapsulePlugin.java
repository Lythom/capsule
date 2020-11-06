package capsule.plugins.jei;

import capsule.Main;
import capsule.blocks.CapsuleBlocks;
import capsule.items.CapsuleItem;
import capsule.items.CapsuleItems;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.ingredients.subtypes.ISubtypeInterpreter;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.ISubtypeRegistration;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.nbt.ByteNBT;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;


@JeiPlugin
public class CapsulePlugin implements IModPlugin {

    @Override
    public void registerItemSubtypes(ISubtypeRegistration subtypeRegistry) {
        subtypeRegistry.registerSubtypeInterpreter(CapsuleItems.CAPSULE, new CapsuleSubtypeInterpreter());
    }

    @Override
    public void registerRecipes(@Nonnull IRecipeRegistration registry) {

        // normally you should ignore nbt per-item, but these tags are universally understood
        // and apply to many vanilla and modded items
        List<IRecipe> recipes = new ArrayList<>();


// TODO re-add upgrade recipes
//        Ingredient upgradeIngredient = CapsuleItems.upgradedCapsule.getValue().upgradeIngredient;
//        for (ItemStack capsule : CapsuleItems.capsuleList.keySet()) {
//            for (int upLevel = 1; upLevel < Math.min(8, Config.upgradeLimit.get()); upLevel++) {
//                ItemStack capsuleUp = CapsuleItems.getUpgradedCapsule(capsule, upLevel);
//                NonNullList<Ingredient> ingredients = NonNullList.withSize(upLevel + 1, upgradeIngredient);
//                ingredients.set(0, Ingredient.fromStacks(capsule));
//                recipes.add(new ShapelessRecipe(new ResourceLocation(Main.MODID, "capsule"), "capsule", capsuleUp, ingredients));
//            }
//            // clear
//            recipes.add(new ShapelessRecipe(new ResourceLocation(Main.MODID, "capsule"), "capsule", capsule, NonNullList.from(Ingredient.EMPTY, Ingredient.fromStacks(CapsuleItems.getUnlabelledCapsule(capsule)))));
//        }

//        ItemStack recoveryCapsule = CapsuleItems.recoveryCapsule.getKey();
//        ItemStack unlabelled = CapsuleItems.unlabelledCapsule.getKey();
//        ItemStack unlabelledDeployed = unlabelled.copy();
//        CapsuleItem.setState(unlabelledDeployed, CapsuleItem.STATE_DEPLOYED);
//        List<ItemStack> blueprintCapsules = CapsuleItems.blueprintCapsules.stream().map(Pair::getKey).collect(Collectors.toList());
//        blueprintCapsules.add(CapsuleItems.blueprintChangedCapsule.getKey());
//        Ingredient anyBlueprint = Ingredient.fromStacks(blueprintCapsules.toArray(new ItemStack[0]));
//        Ingredient unlabelledIng = Ingredient.merge(Arrays.asList(Ingredient.fromStacks(unlabelled), anyBlueprint, Ingredient.fromStacks(recoveryCapsule)));
//        // recovery
//        recipes.add(CapsuleItems.recoveryCapsule.getValue().recipe);

        // blueprint
//        for (Pair<ItemStack, ICraftingRecipe> r : CapsuleItems.blueprintCapsules) {
//            if (r.getValue() instanceof BlueprintCapsuleRecipe) {
//                recipes.add(((BlueprintCapsuleRecipe) r.getValue()).recipe);
//            } else if (r.getValue() instanceof PrefabsBlueprintAggregatorRecipe.PrefabsBlueprintCapsuleRecipe) {
//                recipes.add((( PrefabsBlueprintAggregatorRecipe.PrefabsBlueprintCapsuleRecipe) r.getValue()).recipe);
//            } else {
//                recipes.add(r.getValue());
//            }
//        }
//        ItemStack withNewTemplate = CapsuleItems.blueprintChangedCapsule.getKey();
//        CapsuleItem.setStructureName(withNewTemplate, "newTemplate");
//        CapsuleItem.setLabel(withNewTemplate, "Changed Template");
//        recipes.add(new ShapelessRecipe(new ResourceLocation(Main.MODID, "capsule"),"capsule", withNewTemplate, NonNullList.from(Ingredient.EMPTY, anyBlueprint, unlabelledIng)));

        // registry.addRecipes(recipes, VanillaRecipeCategoryUid.CRAFTING);
        // registry.addIngredientInfo(new ArrayList<>(CapsuleItems.capsuleList.keySet()), VanillaTypes.ITEM, "jei.capsule.desc.capsule");
        registry.addIngredientInfo(CapsuleItems.withState(CapsuleItem.STATE_LINKED), VanillaTypes.ITEM, "jei.capsule.desc.linkedCapsule");
        registry.addIngredientInfo(CapsuleItems.withState(CapsuleItem.STATE_DEPLOYED), VanillaTypes.ITEM, "jei.capsule.desc.linkedCapsule");
        registry.addIngredientInfo(CapsuleItems.withState(CapsuleItem.STATE_ONE_USE), VanillaTypes.ITEM, "jei.capsule.desc.recoveryCapsule");
        registry.addIngredientInfo(CapsuleItems.withState(CapsuleItem.STATE_BLUEPRINT), VanillaTypes.ITEM, "jei.capsule.desc.blueprintCapsule");
        ItemStack opCapsule = CapsuleItems.withState(CapsuleItem.STATE_EMPTY);
        opCapsule.setTagInfo("overpowered", ByteNBT.valueOf(true));
        registry.addIngredientInfo(opCapsule, VanillaTypes.ITEM, "jei.capsule.desc.opCapsule");
        registry.addIngredientInfo(new ItemStack(CapsuleBlocks.CAPSULE_MARKER), VanillaTypes.ITEM, "jei.capsule.desc.capsuleMarker");
    }

    @Override
    public ResourceLocation getPluginUid() {
        return new ResourceLocation(Main.MODID, "main");
    }


    private static class CapsuleSubtypeInterpreter implements ISubtypeInterpreter {
        @Override
        public String apply(ItemStack itemStack) {
            if (!(itemStack.getItem() instanceof CapsuleItem)) return null;
            String isOP = String.valueOf(itemStack.getOrCreateTag().getBoolean("overpowered"));
            String capsuleState = String.valueOf(itemStack.getDamage());
            String capsuleColor = String.valueOf(CapsuleItem.getMaterialColor(itemStack));
            String capsuleBlueprint = String.valueOf(CapsuleItem.isBlueprint(itemStack));
            String label = CapsuleItem.getLabel(itemStack);
            return capsuleState + capsuleColor + isOP + capsuleBlueprint + label;
        }
    }
}
