package capsule.recipes;

import capsule.items.CapsuleItem;
import com.google.gson.JsonObject;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.NonNullList;
import net.minecraft.world.World;
import net.minecraftforge.common.crafting.IRecipeFactory;
import net.minecraftforge.common.crafting.JsonContext;
import net.minecraftforge.oredict.ShapedOreRecipe;

public class RecoveryCapsuleRecipeFactory implements IRecipeFactory {

    @Override
    public IRecipe parse(JsonContext context, JsonObject json) {
        return new RecoveryCapsuleRecipe(ShapedOreRecipe.factory(context, json));
    }

    class RecoveryCapsuleRecipe extends net.minecraftforge.registries.IForgeRegistryEntry.Impl<IRecipe> implements IRecipe {
        private final ShapedOreRecipe recipe;

        public RecoveryCapsuleRecipe(ShapedOreRecipe recipe) {
            this.recipe = recipe;
        }

        public ItemStack getRecipeOutput() {
            return ItemStack.EMPTY;
        }

        /**
         * The original capsule remains in the crafting grid
         */
        public NonNullList<ItemStack> getRemainingItems(InventoryCrafting inv) {
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
        public boolean matches(InventoryCrafting inv, World worldIn) {
            return recipe.matches(inv, worldIn);
        }

        /**
         * Returns a one use copy of the original capsule.
         */
        public ItemStack getCraftingResult(InventoryCrafting invC) {
            for (int i = 0; i < invC.getHeight(); ++i) {
                for (int j = 0; j < invC.getWidth(); ++j) {
                    ItemStack itemstack = invC.getStackInRowAndColumn(j, i);

                    if (CapsuleItem.isLinkedStateCapsule(itemstack)) {
                        ItemStack copy = itemstack.copy();
                        CapsuleItem.setOneUse(copy);
                        return copy;
                    }
                }
            }
            return ItemStack.EMPTY;
        }

        @Override
        public boolean canFit(int width, int height) {
            return recipe.canFit(width, height);
        }

        public boolean isDynamic() {
            return true;
        }
    }
}