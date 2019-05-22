package capsule.recipes;

import capsule.items.CapsuleItem;
import capsule.items.CapsuleItems;
import com.google.gson.JsonObject;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.NonNullList;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.crafting.IRecipeFactory;
import net.minecraftforge.common.crafting.JsonContext;

import static capsule.items.CapsuleItem.STATE_DEPLOYED;

public class BlueprintChangeRecipeFactory implements IRecipeFactory {

    @Override
    public IRecipe parse(JsonContext context, JsonObject json) {
        return new BlueprintChangeRecipe();
    }

    public class BlueprintChangeRecipe extends net.minecraftforge.registries.IForgeRegistryEntry.Impl<IRecipe> implements IRecipe {

        private WorldServer world;

        public BlueprintChangeRecipe() {
        }

        public ItemStack getRecipeOutput() {
            ItemStack bp = new ItemStack(CapsuleItems.capsule, 1, STATE_DEPLOYED);
            CapsuleItem.setBlueprint(bp);
            CapsuleItem.setBaseColor(bp, 3949738);
            CapsuleItem.setStructureName(bp, "blueprintExampleStructureName");
            return bp;
        }

        public NonNullList<ItemStack> getRemainingItems(InventoryCrafting inv) {
            NonNullList<ItemStack> nonnulllist = NonNullList.<ItemStack>withSize(inv.getSizeInventory(), ItemStack.EMPTY);

            ItemStack blueprintCapsule = null;
            ItemStack templateCapsule = null;
            for (int i = 0; i < inv.getHeight(); ++i) {
                for (int j = 0; j < inv.getWidth(); ++j) {
                    ItemStack itemstack = inv.getStackInRowAndColumn(j, i);
                    if (blueprintCapsule == null && CapsuleItem.isBlueprint(itemstack)) {
                        blueprintCapsule = itemstack;
                    } else if (CapsuleItem.hasStructureLink(itemstack)) {
                        templateCapsule = itemstack;
                        nonnulllist.set(i, templateCapsule.copy());
                    }
                }
            }

            return nonnulllist;
        }

        /**
         * Used to check if a recipe matches current crafting inventory
         */
        public boolean matches(InventoryCrafting inv, World worldIn) {
            if (!worldIn.isRemote) {
                this.world = (WorldServer) worldIn;
            }
            int sourceCapsule = 0;
            int blueprint = 0;
            for (int i = 0; i < inv.getHeight(); ++i) {
                for (int j = 0; j < inv.getWidth(); ++j) {
                    ItemStack itemstack = inv.getStackInRowAndColumn(j, i);

                    if (blueprint == 0 && CapsuleItem.isBlueprint(itemstack)) {
                        blueprint++;
                    } else if (CapsuleItem.hasStructureLink(itemstack)) {
                        sourceCapsule++;
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
                    if (blueprintCapsule == null && CapsuleItem.isBlueprint(itemstack)) {
                        blueprintCapsule = itemstack.copy();
                    } else if (CapsuleItem.hasStructureLink(itemstack)) {
                        templateStructure = CapsuleItem.getStructureName(itemstack);
                    }
                }
            }
            if (templateStructure != null && blueprintCapsule != null) {
                blueprintCapsule.getTagCompound().setString("prevStructureName", CapsuleItem.getStructureName(blueprintCapsule));
                CapsuleItem.setStructureName(blueprintCapsule, templateStructure);
                blueprintCapsule.getTagCompound().removeTag("occupiedSpawnPositions");
                blueprintCapsule.getTagCompound().removeTag("spawnPosition");
                CapsuleItem.setState(blueprintCapsule, STATE_DEPLOYED);
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