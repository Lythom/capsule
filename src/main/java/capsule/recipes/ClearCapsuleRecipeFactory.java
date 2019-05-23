package capsule.recipes;

import capsule.items.CapsuleItem;
import capsule.items.CapsuleItems;
import com.google.gson.JsonObject;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.NonNullList;
import net.minecraft.world.World;
import net.minecraftforge.common.crafting.IRecipeFactory;
import net.minecraftforge.common.crafting.JsonContext;

public class ClearCapsuleRecipeFactory implements IRecipeFactory {

    @Override
    public IRecipe parse(JsonContext context, JsonObject json) {
        return new ClearCapsuleRecipe();
    }

    public class ClearCapsuleRecipe extends net.minecraftforge.registries.IForgeRegistryEntry.Impl<IRecipe> implements IRecipe {

        public ClearCapsuleRecipe() {
        }

        public ItemStack getRecipeOutput() {
            return new ItemStack(CapsuleItems.capsule, 1, CapsuleItem.STATE_EMPTY);
        }

        public NonNullList<ItemStack> getRemainingItems(InventoryCrafting inv) {
            NonNullList<ItemStack> nonnulllist = NonNullList.<ItemStack>withSize(inv.getSizeInventory(), ItemStack.EMPTY);

            for (int i = 0; i < nonnulllist.size(); ++i) {
                ItemStack itemstack = inv.getStackInSlot(i);
                nonnulllist.set(i, net.minecraftforge.common.ForgeHooks.getContainerItem(itemstack));
                if (itemstack.getItem() instanceof CapsuleItem && itemstack.getItemDamage() != CapsuleItem.STATE_DEPLOYED) {
                    // Copy the capsule and give back a recovery capsule of the previous content
                    ItemStack copy = itemstack.copy();
                    CapsuleItem.setOneUse(copy);
                    nonnulllist.set(i, copy);
                }
            }

            return nonnulllist;
        }

        /**
         * Used to check if a recipe matches current crafting inventory
         */
        public boolean matches(InventoryCrafting inv, World worldIn) {
            int sourceCapsule = 0;
            for (int i = 0; i < inv.getHeight(); ++i) {
                for (int j = 0; j < inv.getWidth(); ++j) {
                    ItemStack itemstack = inv.getStackInRowAndColumn(j, i);

                    if (canBeEmptyCapsule(itemstack)) {
                        sourceCapsule++;
                    } else if (!itemstack.isEmpty()) {
                        return false;
                    }
                }
            }

            return sourceCapsule == 1;
        }

        public boolean canBeEmptyCapsule(ItemStack itemstack) {
            return CapsuleItem.isLinkedStateCapsule(itemstack) || (itemstack.getItemDamage() == CapsuleItem.STATE_DEPLOYED && !CapsuleItem.isBlueprint(itemstack));
        }

        /**
         * Returns an Item that is the result of this recipe
         */
        public ItemStack getCraftingResult(InventoryCrafting inv) {
            for (int i = 0; i < inv.getHeight(); ++i) {
                for (int j = 0; j < inv.getWidth(); ++j) {
                    ItemStack itemstack = inv.getStackInRowAndColumn(j, i);

                    if (canBeEmptyCapsule(itemstack)) {
                        ItemStack copy = itemstack.copy();
                        CapsuleItem.clearCapsule(copy);
                        return copy;
                    }
                }
            }
            return ItemStack.EMPTY;
        }

        @Override
        public boolean canFit(int width, int height) {
            return width * height >= 1;
        }


        public boolean isDynamic() {
            return true;
        }

        /**
         * Returns the size of the recipe area
         */
        public int getRecipeSize() {
            return 4;
        }
    }
}