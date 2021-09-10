package capsule.plugins.jei;

import capsule.CapsuleMod;
import capsule.Config;
import capsule.blocks.CapsuleBlocks;
import capsule.items.CapsuleItem;
import capsule.items.CapsuleItems;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaRecipeCategoryUid;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.ingredients.subtypes.ISubtypeInterpreter;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.ISubtypeRegistration;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.ICraftingRecipe;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.item.crafting.ShapelessRecipe;
import net.minecraft.nbt.ByteNBT;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static capsule.items.CapsuleItem.CapsuleState.DEPLOYED;
import static capsule.items.CapsuleItem.CapsuleState.EMPTY;

@JeiPlugin
public class CapsulePlugin implements IModPlugin {

    protected static final Logger LOGGER = LogManager.getLogger(CapsulePlugin.class);

    @Override
    public void registerItemSubtypes(ISubtypeRegistration subtypeRegistry) {
        subtypeRegistry.registerSubtypeInterpreter(CapsuleItems.CAPSULE, new CapsuleSubtypeInterpreter());
    }

    @Override
    public void registerRecipes(@Nonnull IRecipeRegistration registry) {

        // normally you should ignore nbt per-item, but these tags are universally understood
        // and apply to many vanilla and modded items
        List<IRecipe> recipes = new ArrayList<>();

        Ingredient upgradeIngredient = CapsuleItems.upgradedCapsule.getValue().upgradeIngredient;
        for (ItemStack capsule : CapsuleItems.capsuleList.keySet()) {
            for (int upLevel = 1; upLevel < Math.min(8, Config.upgradeLimit); upLevel++) {
                ItemStack capsuleUp = CapsuleItems.getUpgradedCapsule(capsule, upLevel);
                NonNullList<Ingredient> ingredients = NonNullList.withSize(upLevel + 1, upgradeIngredient);
                ingredients.set(0, Ingredient.of(capsule));
                recipes.add(new ShapelessRecipe(new ResourceLocation(CapsuleMod.MODID, "capsule"), "capsule", capsuleUp, ingredients));
            }
            // clear
            recipes.add(new ShapelessRecipe(new ResourceLocation(CapsuleMod.MODID, "capsule"), "capsule", capsule, NonNullList.of(Ingredient.EMPTY, Ingredient.of(CapsuleItems.getUnlabelledCapsule(capsule)))));
        }

        if (CapsuleItems.recoveryCapsule == null ||
                CapsuleItems.unlabelledCapsule == null ||
                CapsuleItems.deployedCapsule == null ||
                CapsuleItems.blueprintChangedCapsule == null) {
            LOGGER.error("Some required capsule recipe is missing (recovery, regular capsules or blueprintChanged recipe). The datapack might be corrupted for capsule or the recipe have been remove. JEI won't display the items and capsule might break at some points.");
            return;
        }

        ItemStack recoveryCapsule = CapsuleItems.recoveryCapsule.getKey();
        ItemStack unlabelled = CapsuleItems.unlabelledCapsule.getKey();
        ItemStack unlabelledDeployed = CapsuleItems.deployedCapsule.getKey();
        CapsuleItem.setState(unlabelledDeployed, DEPLOYED);
        Ingredient anyBlueprint = Ingredient.of(CapsuleItems.blueprintCapsules.stream().map(Pair::getKey).toArray(ItemStack[]::new));
        Ingredient unlabelledIng = Ingredient.merge(Arrays.asList(Ingredient.of(unlabelled), anyBlueprint, Ingredient.of(recoveryCapsule)));
        // recovery
        recipes.add(CapsuleItems.recoveryCapsule.getValue().recipe);
        for (Pair<ItemStack, ICraftingRecipe> r : CapsuleItems.blueprintCapsules) {
            recipes.add(r.getValue());
        }
        for (Pair<ItemStack, ICraftingRecipe> r : CapsuleItems.blueprintPrefabs) {
            recipes.add(r.getValue());
        }
        ItemStack withNewTemplate = CapsuleItems.blueprintChangedCapsule.getKey();
        CapsuleItem.setStructureName(withNewTemplate, "newTemplate");
        CapsuleItem.setLabel(withNewTemplate, "Changed Template");
        recipes.add(new ShapelessRecipe(new ResourceLocation(CapsuleMod.MODID, "capsule"), "capsule", withNewTemplate, NonNullList.of(Ingredient.EMPTY, anyBlueprint, unlabelledIng)));

        registry.addRecipes(recipes, VanillaRecipeCategoryUid.CRAFTING);
        registry.addIngredientInfo(new ArrayList<>(CapsuleItems.capsuleList.keySet()), VanillaTypes.ITEM, "jei.capsule.desc.capsule");
        registry.addIngredientInfo(CapsuleItems.blueprintChangedCapsule.getKey(), VanillaTypes.ITEM, "jei.capsule.desc.blueprintCapsule");
        registry.addIngredientInfo(CapsuleItems.unlabelledCapsule.getKey(), VanillaTypes.ITEM, "jei.capsule.desc.linkedCapsule");
        registry.addIngredientInfo(CapsuleItems.deployedCapsule.getKey(), VanillaTypes.ITEM, "jei.capsule.desc.linkedCapsule");
        registry.addIngredientInfo(CapsuleItems.recoveryCapsule.getKey(), VanillaTypes.ITEM, "jei.capsule.desc.recoveryCapsule");
        for (Pair<ItemStack, ICraftingRecipe> blueprintCapsule : CapsuleItems.blueprintCapsules) {
            registry.addIngredientInfo(blueprintCapsule.getKey(), VanillaTypes.ITEM, "jei.capsule.desc.blueprintCapsule");
        }
        for (Pair<ItemStack, ICraftingRecipe> blueprintCapsule : CapsuleItems.blueprintPrefabs) {
            registry.addIngredientInfo(blueprintCapsule.getKey(), VanillaTypes.ITEM, "jei.capsule.desc.blueprintCapsule");
        }
        ItemStack opCapsule = CapsuleItems.withState(EMPTY);
        opCapsule.addTagElement("overpowered", ByteNBT.valueOf(true));
        registry.addIngredientInfo(opCapsule, VanillaTypes.ITEM, "jei.capsule.desc.opCapsule");
        registry.addIngredientInfo(new ItemStack(CapsuleBlocks.CAPSULE_MARKER), VanillaTypes.ITEM, "jei.capsule.desc.capsuleMarker");
    }

    @Override
    public ResourceLocation getPluginUid() {
        return new ResourceLocation(CapsuleMod.MODID, "main");
    }


    private static class CapsuleSubtypeInterpreter implements ISubtypeInterpreter {
        @Override
        public String apply(ItemStack itemStack) {
            if (!(itemStack.getItem() instanceof CapsuleItem)) return null;
            String isOP = String.valueOf(itemStack.hasTag() && itemStack.getTag().getBoolean("overpowered"));
            String capsuleState = String.valueOf(CapsuleItem.getState(itemStack));
            String capsuleColor = String.valueOf(CapsuleItem.getMaterialColor(itemStack));
            String capsuleBlueprint = String.valueOf(CapsuleItem.isBlueprint(itemStack));
            ITextComponent labelComp = CapsuleItem.getLabel(itemStack);
            String label = labelComp == null ? "" : labelComp.getString();
            return capsuleState + capsuleColor + isOP + capsuleBlueprint + label;
        }
    }
}
