package capsule.items.recipes;

import capsule.items.CapsuleItem;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.NonNullList;
import net.minecraft.world.World;

public class RecoveryCapsuleRecipe implements IRecipe {
    /**
     * Is the ItemStack that you repair.
     */
    private final ItemStack inputCapsule;
    private final ItemStack inputMaterial;
    private final int targetMetadata;

    public RecoveryCapsuleRecipe(ItemStack inputCapsule, ItemStack inputMaterial, int targetMetadata) {
        this.inputCapsule = inputCapsule;
        this.inputMaterial = inputMaterial;
        this.targetMetadata = targetMetadata;
    }

    public ItemStack getRecipeOutput() {
        return ItemStack.EMPTY;
    }

    public NonNullList<ItemStack> getRemainingItems(InventoryCrafting inv) {
        NonNullList<ItemStack> nonnulllist = NonNullList.withSize(inv.getSizeInventory(), ItemStack.EMPTY);

        for (int i = 0; i < nonnulllist.size(); ++i) {
            ItemStack itemstack = inv.getStackInSlot(i);
            nonnulllist.set(i, net.minecraftforge.common.ForgeHooks.getContainerItem(itemstack));
            if (itemstack.getItem() instanceof CapsuleItem){
                nonnulllist.set(i, itemstack.copy());
            }
        }

        return nonnulllist;
    }

    /**
     * Used to check if a recipe matches current crafting inventory
     */
    public boolean matches(InventoryCrafting inv, World worldIn) {
        int sourceCapsule = 0;
        int material = 0;
        for (int i = 0; i < inv.getHeight(); ++i) {
            for (int j = 0; j < inv.getWidth(); ++j) {
                ItemStack itemstack = inv.getStackInRowAndColumn(j, i);

                if (!itemstack.isEmpty() && itemstack.getItem() == this.inputCapsule.getItem() && (itemstack.getMetadata() == this.inputCapsule.getMetadata())) {
                    sourceCapsule++;
                } else if (!itemstack.isEmpty() && itemstack.getItem() == this.inputMaterial.getItem() && (itemstack.getMetadata() == this.inputMaterial.getMetadata())) {
                    material++;
                } else if (!itemstack.isEmpty()) {
                    return false;
                }
            }
        }

        return sourceCapsule == 1 && material == 1;
    }

    /**
     * Returns an Item that is the result of this recipe
     */
    public ItemStack getCraftingResult(InventoryCrafting invC) {
        for (int i = 0; i < invC.getHeight(); ++i) {
            for (int j = 0; j < invC.getWidth(); ++j) {
                ItemStack itemstack = invC.getStackInRowAndColumn(j, i);

                if (!itemstack.isEmpty() && itemstack.getItem() == this.inputCapsule.getItem() && itemstack.getMetadata() == this.inputCapsule.getMetadata()) {
                    ItemStack copy = itemstack.copy();
                    CapsuleItem.setState(copy, this.targetMetadata);
                    CapsuleItem.setOneUse(copy);
                    return copy;
                }
            }
        }
        return ItemStack.EMPTY;
    }

    /**
     * Returns the size of the recipe area
     */
    public int getRecipeSize() {
        return 4;
    }
}