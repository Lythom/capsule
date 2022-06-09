package capsule.recipes;

import capsule.helpers.Capsule;
import capsule.items.CapsuleItem;
import com.google.gson.JsonObject;
import net.minecraft.core.NonNullList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;

import static capsule.items.CapsuleItem.CapsuleState.DEPLOYED;

/**
 * Handle 2 cases :
 * crafting a blueprint from a source capsule, in this case the structureName to create template is taken from the source capsule
 * crafting a prefab, in this case the structureName to create template is taken from the recipe output
 */
public class BlueprintCapsuleRecipe implements CraftingRecipe {
    public final ShapedRecipe recipe;

    public BlueprintCapsuleRecipe(ShapedRecipe recipe) {
        this.recipe = recipe;
    }

    @Override
    public ItemStack getResultItem() {
        return recipe.getResultItem();
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> ingredients = recipe.getIngredients();
        return ingredients;
    }

    /**
     * The original capsule remains in the crafting grid
     */
    public NonNullList<ItemStack> getRemainingItems(CraftingContainer inv) {
        NonNullList<ItemStack> nonnulllist = NonNullList.withSize(inv.getContainerSize(), ItemStack.EMPTY);

        for (int i = 0; i < nonnulllist.size(); ++i) {
            ItemStack itemstack = inv.getItem(i);
            nonnulllist.set(i, net.minecraftforge.common.ForgeHooks.getContainerItem(itemstack));
            if (itemstack.getItem() instanceof CapsuleItem) {
                nonnulllist.set(i, itemstack.copy());
            }
        }

        return nonnulllist;
    }

    private boolean IsCopyable(ItemStack itemstack) {
        return CapsuleItem.isLinkedStateCapsule(itemstack) || CapsuleItem.isBlueprint(itemstack) || CapsuleItem.isOneUse(itemstack);
    }

    /**
     * Used to check if a recipe matches current crafting inventory
     */
    public boolean matches(CraftingContainer inv, Level worldIn) {
        if (!recipe.matches(inv, worldIn)) {
            return false;
        }

        // in case it's not a prefab but a copy template recipe
        for (int i = 0; i < inv.getContainerSize(); ++i) {
            ItemStack itemstack = inv.getItem(i);
            if (itemstack.getItem() instanceof CapsuleItem && !IsCopyable(itemstack)) {
                return false;
            }
        }
        return true;
    }


    /**
     * Returns a copy built from the original capsule.
     */
    public ItemStack assemble(CraftingContainer inv) {
        ItemStack referenceCapsule = recipe.getResultItem();
        for (int i = 0; i < inv.getContainerSize(); ++i) {
            ItemStack itemstack = inv.getItem(i);
            if (IsCopyable(itemstack)) {
                // This blueprint will take the source structure name by copying it here
                // a new dedicated template is created later.
                // @see CapsuleItem.onCreated
                referenceCapsule = itemstack;
            }

        }
        try {
            ItemStack blueprintItem = Capsule.newLinkedCapsuleItemStack(
                    CapsuleItem.getStructureName(referenceCapsule),
                    CapsuleItem.getBaseColor(recipe.getResultItem()),
                    0xFFFFFF,
                    CapsuleItem.getSize(referenceCapsule),
                    CapsuleItem.isOverpowered(referenceCapsule),
                    referenceCapsule.getTag() != null ? referenceCapsule.getTag().getString("label") : null,
                    0
            );
            CapsuleItem.setBlueprint(blueprintItem);
            // hack to force a tempalte copy if it's not done after craft
            if (blueprintItem.getTag() != null) {
                blueprintItem.getTag().putBoolean("templateShouldBeCopied", true);
            }
            CapsuleItem.setState(blueprintItem, DEPLOYED);
            return blueprintItem;
        } catch (Exception e) {
            e.printStackTrace();
            return ItemStack.EMPTY;
        }
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return recipe.canCraftInDimensions(width, height);
    }

    @Override
    public ResourceLocation getId() {
        return recipe.getId();
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return CapsuleRecipes.BLUEPRINT_CAPSULE_SERIALIZER.get();
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    public static class Serializer implements RecipeSerializer<BlueprintCapsuleRecipe> {
        @Override
        public BlueprintCapsuleRecipe fromJson(ResourceLocation recipeId, JsonObject json) {
            return new BlueprintCapsuleRecipe(ShapedRecipe.Serializer.SHAPED_RECIPE.fromJson(recipeId, json));
        }

        @Override
        public BlueprintCapsuleRecipe fromNetwork(ResourceLocation recipeId, FriendlyByteBuf buffer) {
            return new BlueprintCapsuleRecipe(ShapedRecipe.Serializer.SHAPED_RECIPE.fromNetwork(recipeId, buffer));
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, BlueprintCapsuleRecipe recipe) {
            ShapedRecipe.Serializer.SHAPED_RECIPE.toNetwork(buffer, recipe.recipe);
        }
    }
}