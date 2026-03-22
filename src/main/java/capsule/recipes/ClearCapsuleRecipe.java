package capsule.recipes;

import capsule.items.CapsuleItem;
import capsule.items.CapsuleItems;
import net.minecraft.core.NonNullList;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.CommonHooks;

import static capsule.items.CapsuleItem.CapsuleState.DEPLOYED;
import static capsule.items.CapsuleItem.CapsuleState.EMPTY;

public class ClearCapsuleRecipe extends CustomRecipe {

    public ClearCapsuleRecipe(CraftingBookCategory category) {
        super(category);
    }


    @Override
    public RecipeSerializer<?> getSerializer() {
        return CapsuleRecipes.CLEAR_CAPSULE_SERIALIZER.get();
    }

    /**
     * Used to check if a recipe matches current crafting inventory
     */
    public boolean matches(CraftingInput inv, Level worldIn) {
        int sourceCapsule = 0;
        NonNullList<ItemStack> nonnulllist = NonNullList.withSize(inv.size(), ItemStack.EMPTY);
        for (int i = 0; i < nonnulllist.size(); ++i) {
            ItemStack itemstack = inv.getItem(i);

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
    public ItemStack assemble(CraftingInput inv, HolderLookup.Provider registryAccess) {
        NonNullList<ItemStack> nonnulllist = NonNullList.withSize(inv.size(), ItemStack.EMPTY);

        for (int i = 0; i < nonnulllist.size(); ++i) {
            ItemStack itemstack = inv.getItem(i);

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
        return CapsuleItem.isLinkedStateCapsule(itemstack) || (CapsuleItem.hasState(itemstack, DEPLOYED) && !CapsuleItem.isBlueprint(itemstack));
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 1;
    }

    public ItemStack getResultItem(HolderLookup.Provider registryAccess) {
        return CapsuleItems.withState(EMPTY);
    }

    public NonNullList<ItemStack> getRemainingItems(CraftingInput inv) {
        NonNullList<ItemStack> nonnulllist = NonNullList.withSize(inv.size(), ItemStack.EMPTY);

        for (int i = 0; i < nonnulllist.size(); ++i) {
            ItemStack itemstack = inv.getItem(i);
            nonnulllist.set(i, CommonHooks.getCraftingRemainingItem(itemstack));
            if (itemstack.getItem() instanceof CapsuleItem && !CapsuleItem.hasState(itemstack, DEPLOYED)) {
                // Copy the capsule and give back a recovery capsule of the previous content
                ItemStack copy = itemstack.copy();
                CapsuleItem.setOneUse(copy);
                nonnulllist.set(i, copy);
            }
        }

        return nonnulllist;
    }
}