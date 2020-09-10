package capsule.recipes;

import capsule.items.CapsuleItem;
import capsule.items.CapsuleItems;
import com.google.gson.JsonObject;
import net.minecraft.inventory.CraftingInventory;
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
            ItemStack bp = new ItemStack(CapsuleItems.capsule, 1, CapsuleItem.STATE_DEPLOYED);
            CapsuleItem.setBlueprint(bp);
            CapsuleItem.setBaseColor(bp, 3949738);
            CapsuleItem.setStructureName(bp, "blueprintExampleStructureName");
            return bp;
        }

        public NonNullList<ItemStack> getRemainingItems(CraftingInventory inv) {
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
                        nonnulllist.set(3 * i + j, templateCapsule.copy());
                    }
                }
            }

            return nonnulllist;
        }

        /**
         * Used to check if a recipe matches current crafting inventory
         */
        public boolean matches(CraftingInventory inv, World worldIn) {
            int sourceCapsule = 0;
            int blueprint = 0;
            for (int i = 0; i < inv.getHeight(); ++i) {
                for (int j = 0; j < inv.getWidth(); ++j) {
                    ItemStack itemstack = inv.getStackInRowAndColumn(j, i);

                    if (blueprint == 0 && CapsuleItem.isBlueprint(itemstack)) {
                        blueprint++;

                        // Any capsule having a template is valid except Deployed capsules (empty template) unless it is a blueprint (template never empty)
                    } else if (CapsuleItem.hasStructureLink(itemstack) && (CapsuleItem.STATE_DEPLOYED != itemstack.getDamage() || CapsuleItem.isBlueprint(itemstack))) {
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
        public ItemStack getCraftingResult(CraftingInventory inv) {
            String templateStructure = null;
            Integer templateSize = null;
            ItemStack blueprintCapsule = null;
            for (int i = 0; i < inv.getHeight(); ++i) {
                for (int j = 0; j < inv.getWidth(); ++j) {
                    ItemStack itemstack = inv.getStackInRowAndColumn(j, i);
                    if (blueprintCapsule == null && CapsuleItem.isBlueprint(itemstack)) {
                        blueprintCapsule = itemstack.copy();
                    } else if (CapsuleItem.hasStructureLink(itemstack)) {
                        templateStructure = CapsuleItem.getStructureName(itemstack);
                        templateSize = CapsuleItem.getSize(itemstack);
                    }
                }
            }
            if (templateStructure != null && blueprintCapsule != null) {
                if (blueprintCapsule.getTag() != null) {
                    blueprintCapsule.getTag().putString("prevStructureName", CapsuleItem.getStructureName(blueprintCapsule));
                }
                CapsuleItem.setStructureName(blueprintCapsule, templateStructure);
                CapsuleItem.setState(blueprintCapsule, CapsuleItem.STATE_DEPLOYED);
                CapsuleItem.setSize(blueprintCapsule, templateSize);
                CapsuleItem.cleanDeploymentTags(blueprintCapsule);
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