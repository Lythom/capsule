package capsule.items.recipes;

import capsule.items.CapsuleItem;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.NonNullList;
import net.minecraft.world.World;

public class ClearCapsuleRecipe implements IRecipe {

    public ClearCapsuleRecipe() {
    }

    public ItemStack getRecipeOutput() {
        return ItemStack.EMPTY;
    }

    public NonNullList<ItemStack> getRemainingItems(InventoryCrafting inv) {
        NonNullList<ItemStack> nonnulllist = NonNullList.<ItemStack>withSize(inv.getSizeInventory(), ItemStack.EMPTY);

        for (int i = 0; i < nonnulllist.size(); ++i) {
            ItemStack itemstack = inv.getStackInSlot(i);
            nonnulllist.set(i, net.minecraftforge.common.ForgeHooks.getContainerItem(itemstack));
            if (itemstack.getItem() instanceof CapsuleItem) {
                // Copy the capsule and give back a recovery capsule of the previous content
                ItemStack copy = itemstack.copy();
                CapsuleItem item = (CapsuleItem) copy.getItem();
                if (item != null) {
                    CapsuleItem.setOneUse(copy);
                    nonnulllist.set(i, copy);
                }
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

                if (isLinkedCapsule(itemstack)) {
                    sourceCapsule++;
                } else if (!itemstack.isEmpty()) {
                    return false;
                }
            }
        }

        return sourceCapsule == 1;
    }

    /**
     * Returns an Item that is the result of this recipe
     */
    public ItemStack getCraftingResult(InventoryCrafting inv) {
        for (int i = 0; i < inv.getHeight(); ++i) {
            for (int j = 0; j < inv.getWidth(); ++j) {
                ItemStack itemstack = inv.getStackInRowAndColumn(j, i);

                if (isLinkedCapsule(itemstack)) {
                    ItemStack copy = itemstack.copy();
                    CapsuleItem item = (CapsuleItem) copy.getItem();
                    if (item != null) {
                        item.clearCapsule(copy);
                    }
                    return copy;
                }
            }
        }
        return ItemStack.EMPTY;
    }

    private boolean isLinkedCapsule(ItemStack itemstack) {
        return (!itemstack.isEmpty() && itemstack.getItem() instanceof CapsuleItem && CapsuleItem.STATE_LINKED == itemstack.getMetadata());
    }

    /**
     * Returns the size of the recipe area
     */
    public int getRecipeSize() {
        return 4;
    }
}