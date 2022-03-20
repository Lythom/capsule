package capsule.recipes;

import capsule.items.CapsuleItem;
import capsule.items.CapsuleItems;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import static capsule.items.CapsuleItem.CapsuleState.DEPLOYED;
import static capsule.items.CapsuleItem.CapsuleState.EMPTY;

public class ClearCapsuleRecipe extends CustomRecipe {

    public ClearCapsuleRecipe(ResourceLocation idIn) {
        super(idIn);
    }


    @Override
    public RecipeSerializer<?> getSerializer() {
        return CapsuleRecipes.CLEAR_CAPSULE_SERIALIZER;
    }

    /**
     * Used to check if a recipe matches current crafting inventory
     */
    public boolean matches(CraftingContainer inv, Level worldIn) {
        int sourceCapsule = 0;
        NonNullList<ItemStack> nonnulllist = NonNullList.withSize(inv.getContainerSize(), ItemStack.EMPTY);
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
    public ItemStack assemble(CraftingContainer inv) {
        NonNullList<ItemStack> nonnulllist = NonNullList.withSize(inv.getContainerSize(), ItemStack.EMPTY);

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

    public ItemStack getResultItem() {
        return CapsuleItems.withState(EMPTY);
    }

    public NonNullList<ItemStack> getRemainingItems(CraftingContainer inv) {
        NonNullList<ItemStack> nonnulllist = NonNullList.withSize(inv.getContainerSize(), ItemStack.EMPTY);

        for (int i = 0; i < nonnulllist.size(); ++i) {
            ItemStack itemstack = inv.getItem(i);
            nonnulllist.set(i, net.minecraftforge.common.ForgeHooks.getContainerItem(itemstack));
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