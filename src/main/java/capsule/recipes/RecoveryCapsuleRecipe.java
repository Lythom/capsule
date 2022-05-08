package capsule.recipes;

import capsule.items.CapsuleItem;
import com.google.gson.JsonObject;
import net.minecraft.core.NonNullList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.Level;

public class RecoveryCapsuleRecipe implements CraftingRecipe {
    public final ShapelessRecipe recipe;

    public RecoveryCapsuleRecipe(ShapelessRecipe recipe) {
        this.recipe = recipe;
    }

    public ItemStack getResultItem() {
        return recipe.getResultItem();
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

    /**
     * Used to check if a recipe matches current crafting inventory
     */
    public boolean matches(CraftingContainer inv, Level worldIn) {
        return recipe.matches(inv, worldIn);
    }

    /**
     * Returns a copy built from the original capsule.
     */
    public ItemStack assemble(CraftingContainer invC) {
        for (int i = 0; i < invC.getContainerSize(); ++i) {
            ItemStack itemstack = invC.getItem(i);

            if (CapsuleItem.isLinkedStateCapsule(itemstack)) {
                ItemStack copy = itemstack.copy();
                CapsuleItem.setOneUse(copy);
                return copy;
            }
        }
        return ItemStack.EMPTY;
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
        return CapsuleRecipes.RECOVERY_CAPSULE_SERIALIZER;
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    public static class Serializer extends net.minecraftforge.registries.ForgeRegistryEntry<RecipeSerializer<?>> implements RecipeSerializer<RecoveryCapsuleRecipe> {

        @Override
        public RecoveryCapsuleRecipe fromJson(ResourceLocation recipeId, JsonObject json) {
            return new RecoveryCapsuleRecipe(ShapelessRecipe.Serializer.SHAPELESS_RECIPE.fromJson(recipeId, json));
        }

        @Override
        public RecoveryCapsuleRecipe fromNetwork(ResourceLocation recipeId, FriendlyByteBuf buffer) {
            return new RecoveryCapsuleRecipe(ShapelessRecipe.Serializer.SHAPELESS_RECIPE.fromNetwork(recipeId, buffer));
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, RecoveryCapsuleRecipe recipe) {
            ShapelessRecipe.Serializer.SHAPELESS_RECIPE.toNetwork(buffer, recipe.recipe);
        }
    }
}