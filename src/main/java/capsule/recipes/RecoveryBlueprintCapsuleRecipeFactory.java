package capsule.recipes;

import capsule.items.CapsuleItem;
import com.google.gson.JsonObject;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.NonNullList;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.crafting.IRecipeFactory;
import net.minecraftforge.common.crafting.JsonContext;
import net.minecraftforge.oredict.ShapedOreRecipe;

import static capsule.items.CapsuleItem.*;

public class RecoveryBlueprintCapsuleRecipeFactory implements IRecipeFactory {

    @Override
    public IRecipe parse(JsonContext context, JsonObject json) {
        return new RecoveryBlueprintCapsuleRecipe(ShapedOreRecipe.factory(context, json));
    }

    public class RecoveryBlueprintCapsuleRecipe extends net.minecraftforge.registries.IForgeRegistryEntry.Impl<IRecipe> implements IRecipe {
        public final ShapedOreRecipe recipe;

        public RecoveryBlueprintCapsuleRecipe(ShapedOreRecipe recipe) {
            this.recipe = recipe;
        }

        public ItemStack getRecipeOutput() {
            return recipe.getRecipeOutput();
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
         * Returns a copy built from the original capsule.
         */
        public ItemStack getCraftingResult(InventoryCrafting invC) {
            ItemStack configuredOutput = recipe.getRecipeOutput();
            for (int i = 0; i < invC.getHeight(); ++i) {
                for (int j = 0; j < invC.getWidth(); ++j) {
                    ItemStack itemstack = invC.getStackInRowAndColumn(j, i);

                    if (CapsuleItem.isLinkedStateCapsule(itemstack)) {
                        if (isOneUse(configuredOutput)) {
                            ItemStack copy = itemstack.copy();
                            CapsuleItem.setOneUse(copy);
                            return copy;
                        } else if (isBlueprint(configuredOutput)) {
                            // This blueprint will take the source structure name by copying it here
                            // a new dedicated template is created later.
                            // @see CapsuleItem.onCreated
                            ItemStack copy = itemstack.copy();
                            CapsuleItem.setMaterialColor(copy, 0xFFFFFF);
                            CapsuleItem.setBaseColor(copy, CapsuleItem.getBaseColor(recipe.getRecipeOutput()));
                            CapsuleItem.setBlueprint(copy);
                            CapsuleItem.setState(copy, STATE_DEPLOYED);
                            CapsuleItem.setUpgradeLevel(copy,0);
                            return copy;
                        }
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