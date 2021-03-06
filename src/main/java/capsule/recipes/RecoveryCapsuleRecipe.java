package capsule.recipes;

import capsule.items.CapsuleItem;
import com.google.gson.JsonObject;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.ICraftingRecipe;
import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraft.item.crafting.ShapelessRecipe;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

public class RecoveryCapsuleRecipe implements ICraftingRecipe {
    public final ShapelessRecipe recipe;

    public RecoveryCapsuleRecipe(ShapelessRecipe recipe) {
        this.recipe = recipe;
    }

    public ItemStack getRecipeOutput() {
        return recipe.getRecipeOutput();
    }

    /**
     * The original capsule remains in the crafting grid
     */
    public NonNullList<ItemStack> getRemainingItems(CraftingInventory inv) {
        NonNullList<ItemStack> nonnulllist = NonNullList.withSize(inv.getSizeInventory(), ItemStack.EMPTY);

        for (int i = 0; i < nonnulllist.size(); ++i) {
            ItemStack itemstack = inv.getStackInSlot(i);
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
    public boolean matches(CraftingInventory inv, World worldIn) {
        return recipe.matches(inv, worldIn);
    }

    /**
     * Returns a copy built from the original capsule.
     */
    public ItemStack getCraftingResult(CraftingInventory invC) {
        for (int i = 0; i < invC.getSizeInventory(); ++i) {
            ItemStack itemstack = invC.getStackInSlot(i);

            if (CapsuleItem.isLinkedStateCapsule(itemstack)) {
                ItemStack copy = itemstack.copy();
                CapsuleItem.setOneUse(copy);
                return copy;
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canFit(int width, int height) {
        return recipe.canFit(width, height);
    }

    @Override
    public ResourceLocation getId() {
        return recipe.getId();
    }

    @Override
    public IRecipeSerializer<?> getSerializer() {
        return CapsuleRecipes.RECOVERY_CAPSULE_SERIALIZER;
    }

    @Override
    public boolean isDynamic() {
        return true;
    }

    public static class Serializer extends net.minecraftforge.registries.ForgeRegistryEntry<IRecipeSerializer<?>> implements IRecipeSerializer<RecoveryCapsuleRecipe> {

        @Override
        public RecoveryCapsuleRecipe read(ResourceLocation recipeId, JsonObject json) {
            return new RecoveryCapsuleRecipe(ShapelessRecipe.Serializer.CRAFTING_SHAPELESS.read(recipeId, json));
        }

        @Override
        public RecoveryCapsuleRecipe read(ResourceLocation recipeId, PacketBuffer buffer) {
            return new RecoveryCapsuleRecipe(ShapelessRecipe.Serializer.CRAFTING_SHAPELESS.read(recipeId, buffer));
        }

        @Override
        public void write(PacketBuffer buffer, RecoveryCapsuleRecipe recipe) {
            ShapelessRecipe.Serializer.CRAFTING_SHAPELESS.write(buffer, recipe.recipe);
        }
    }
}