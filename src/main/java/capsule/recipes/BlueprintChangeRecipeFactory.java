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

public class BlueprintChangeRecipeFactory implements IRecipeFactory {

    @Override
    public IRecipe parse(JsonContext context, JsonObject json) {
        return new BlueprintChangeRecipe();
    }

    public class BlueprintChangeRecipe extends net.minecraftforge.registries.IForgeRegistryEntry.Impl<IRecipe> implements IRecipe {

        public BlueprintChangeRecipe() {
        }

        public ItemStack getRecipeOutput() {
            return new ItemStack(CapsuleItems.capsule, 1, CapsuleItem.STATE_BLUEPRINT);
        }

        public NonNullList<ItemStack> getRemainingItems(InventoryCrafting inv) {
            NonNullList<ItemStack> nonnulllist = NonNullList.<ItemStack>withSize(inv.getSizeInventory(), ItemStack.EMPTY);

            for (int i = 0; i < nonnulllist.size(); ++i) {
                ItemStack itemstack = inv.getStackInSlot(i);
                nonnulllist.set(i, net.minecraftforge.common.ForgeHooks.getContainerItem(itemstack));
                if (itemstack.getItem() instanceof CapsuleItem && CapsuleItem.isLinkedStateCapsule(itemstack)) {
                    // give back the capsule where template is taken from
                    ItemStack copy = itemstack.copy();
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
            int blueprint = 0;
            for (int i = 0; i < inv.getHeight(); ++i) {
                for (int j = 0; j < inv.getWidth(); ++j) {
                    ItemStack itemstack = inv.getStackInRowAndColumn(j, i);

                    if (CapsuleItem.isLinkedStateCapsule(itemstack)) {
                        sourceCapsule++;
                    } else if (CapsuleItem.isBlueprint(itemstack)) {
                        blueprint++;
                    } else if (!itemstack.isEmpty()) {
                        return false;
                    }
                }
            }

            return sourceCapsule == 1 && blueprint == 1;
        }

        /**
         * Returns an Item that is the result of this recipe
         */
        public ItemStack getCraftingResult(InventoryCrafting inv) {
            String templateStructure = null;
            ItemStack blueprintCapsule = null;
            for (int i = 0; i < inv.getHeight(); ++i) {
                for (int j = 0; j < inv.getWidth(); ++j) {
                    ItemStack itemstack = inv.getStackInRowAndColumn(j, i);
                    if (CapsuleItem.isLinkedStateCapsule(itemstack)) {
                        templateStructure = CapsuleItem.getStructureName(itemstack);
                    } else if (CapsuleItem.isBlueprint(itemstack)) {
                        blueprintCapsule = itemstack.copy();
                    }
                }
            }
            if (templateStructure != null && blueprintCapsule != null) {
                CapsuleItem.setStructureName(blueprintCapsule, templateStructure);
                blueprintCapsule.getTagCompound().removeTag("occupiedSpawnPositions");
                blueprintCapsule.getTagCompound().removeTag("spawnPosition");
                return blueprintCapsule;
            }
            return ItemStack.EMPTY;
        }

        @Override
        public boolean canFit(int width, int height) {
            return width * height >= 2;
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