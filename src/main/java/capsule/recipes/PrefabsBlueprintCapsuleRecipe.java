package capsule.recipes;

import capsule.StructureSaver;
import capsule.items.CapsuleItem;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.JsonUtils;
import net.minecraft.util.NonNullList;
import net.minecraft.world.World;
import net.minecraftforge.common.crafting.JsonContext;
import net.minecraftforge.oredict.ShapedOreRecipe;
import org.apache.commons.lang3.tuple.Triple;

import static net.minecraftforge.registries.IForgeRegistryEntry.Impl;

public class PrefabsBlueprintCapsuleRecipe extends Impl<IRecipe> implements IRecipe {
    public final ShapedOreRecipe recipe;
    private int ingredientOneIndex = 4;
    private int ingredientTwoIndex = 0;
    private int ingredientThreeIndex = 2;

    public PrefabsBlueprintCapsuleRecipe(JsonObject template, Triple<StructureSaver.ItemStackKey, StructureSaver.ItemStackKey, StructureSaver.ItemStackKey> ingredients) {
        this.recipe = ShapedOreRecipe.factory(new JsonContext("capsule"), template);
        buildRecipeFromPattern(template, ingredients);
    }

    public void buildRecipeFromPattern(JsonObject template, Triple<StructureSaver.ItemStackKey, StructureSaver.ItemStackKey, StructureSaver.ItemStackKey> ingredients) {
        JsonArray patternArr = JsonUtils.getJsonArray(template, "pattern");
        String pattern = patternArr.get(0).getAsString() + patternArr.get(1).getAsString() + patternArr.get(2).getAsString();
        ingredientOneIndex = pattern.indexOf("1");
        ingredientTwoIndex = pattern.indexOf("2");
        ingredientThreeIndex = pattern.indexOf("3");
        this.recipe.getIngredients().set(ingredientOneIndex, Ingredient.fromStacks(ingredients.getLeft().itemStack));
        if (ingredients.getMiddle() != null)
            this.recipe.getIngredients().set(ingredientTwoIndex, Ingredient.fromStacks(ingredients.getMiddle().itemStack));
        if (ingredients.getRight() != null)
            this.recipe.getIngredients().set(ingredientThreeIndex, Ingredient.fromStacks(ingredients.getRight().itemStack));
    }

    public ItemStack getRecipeOutput() {
        return recipe.getRecipeOutput();
    }

    /**
     * Only blueprint material is consumed. Materials used inside blueprint are given back.
     */
    public NonNullList<ItemStack> getRemainingItems(InventoryCrafting inv) {
        NonNullList<ItemStack> nonnulllist = NonNullList.withSize(inv.getSizeInventory(), ItemStack.EMPTY);

        for (int i = 0; i < nonnulllist.size(); ++i) {
            ItemStack itemstack = inv.getStackInSlot(i);
            nonnulllist.set(i, net.minecraftforge.common.ForgeHooks.getContainerItem(itemstack));
            if (i == ingredientOneIndex || i == ingredientTwoIndex || i == ingredientThreeIndex || itemstack.getItem() instanceof CapsuleItem) {
                nonnulllist.set(i, itemstack.copy());
            }
        }

        return nonnulllist;
    }

    public boolean matches(InventoryCrafting inv, World worldIn) {
        return recipe.matches(inv, worldIn);
    }

    public ItemStack getCraftingResult(InventoryCrafting invC) {
        return recipe.getCraftingResult(invC);
    }

    @Override
    public boolean canFit(int width, int height) {
        return recipe.canFit(width, height);
    }

    public boolean isDynamic() {
        return true;
    }
}