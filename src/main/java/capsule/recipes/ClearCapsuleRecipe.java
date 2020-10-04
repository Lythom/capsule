
package capsule.recipes;

import capsule.CommonProxy;
import capsule.items.CapsuleItem;
import capsule.items.CapsuleItems;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraft.item.crafting.SpecialRecipe;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

public class ClearCapsuleRecipe extends SpecialRecipe {

    public ClearCapsuleRecipe(ResourceLocation idIn) {
        super(idIn);
    }


    @Override
    public IRecipeSerializer<?> getSerializer() {
        return CommonProxy.CLEAR_CAPSULE_SERIALIZER;
    }

    /**
     * Used to check if a recipe matches current crafting inventory
     */
    public boolean matches(CraftingInventory inv, World worldIn) {
        int sourceCapsule = 0;
        NonNullList<ItemStack> nonnulllist = NonNullList.withSize(inv.getSizeInventory(), ItemStack.EMPTY);
        for (int i = 0; i < nonnulllist.size(); ++i) {
            ItemStack itemstack = inv.getStackInSlot(i);

            if (canBeEmptyCapsule(itemstack)) {
                sourceCapsule++;
            } else if (!itemstack.isEmpty()) {
                return false;
            }

        }

        return sourceCapsule == 1;
    }

    /**
     * Returns an Item that is the result of this recipe
     */
    public ItemStack getCraftingResult(CraftingInventory inv) {
        NonNullList<ItemStack> nonnulllist = NonNullList.withSize(inv.getSizeInventory(), ItemStack.EMPTY);

        for (int i = 0; i < nonnulllist.size(); ++i) {
            ItemStack itemstack = inv.getStackInSlot(i);

            if (canBeEmptyCapsule(itemstack)) {
                ItemStack copy = itemstack.copy();
                CapsuleItem.clearCapsule(copy);
                return copy;

            }
        }
        return ItemStack.EMPTY;
    }

    public boolean canBeEmptyCapsule(ItemStack itemstack) {
        if (!(itemstack.getItem() instanceof CapsuleItem)) return false;
        return CapsuleItem.isLinkedStateCapsule(itemstack) || (itemstack.getDamage() == CapsuleItem.STATE_DEPLOYED && !CapsuleItem.isBlueprint(itemstack));
    }

    @Override
    public boolean canFit(int width, int height) {
        return width * height >= 1;
    }

    public ItemStack getRecipeOutput() {
        return CapsuleItems.withState(CapsuleItem.STATE_EMPTY);
    }

    public NonNullList<ItemStack> getRemainingItems(CraftingInventory inv) {
        NonNullList<ItemStack> nonnulllist = NonNullList.<ItemStack>withSize(inv.getSizeInventory(), ItemStack.EMPTY);

        for (int i = 0; i < nonnulllist.size(); ++i) {
            ItemStack itemstack = inv.getStackInSlot(i);
            nonnulllist.set(i, net.minecraftforge.common.ForgeHooks.getContainerItem(itemstack));
            if (itemstack.getItem() instanceof CapsuleItem && itemstack.getDamage() != CapsuleItem.STATE_DEPLOYED) {
                // Copy the capsule and give back a recovery capsule of the previous content
                ItemStack copy = itemstack.copy();
                CapsuleItem.setOneUse(copy);
                nonnulllist.set(i, copy);
            }
        }

        return nonnulllist;
    }
}