package capsule.recipes;

import capsule.helpers.Capsule;
import capsule.items.CapsuleItem;
import com.google.gson.JsonObject;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.NonNullList;
import net.minecraft.world.World;
import net.minecraftforge.common.crafting.IRecipeFactory;
import net.minecraftforge.common.crafting.JsonContext;
import net.minecraftforge.oredict.ShapedOreRecipe;

import static capsule.items.CapsuleItem.*;

public class BlueprintCapsuleRecipeFactory implements IRecipeFactory {

    @Override
    public IRecipe parse(JsonContext context, JsonObject json) {
        return new BlueprintCapsuleRecipe(ShapedOreRecipe.factory(context, json));
    }

    public class BlueprintCapsuleRecipe extends net.minecraftforge.registries.IForgeRegistryEntry.Impl<IRecipe> implements IRecipe {
        public final ShapedOreRecipe recipe;

        public BlueprintCapsuleRecipe(ShapedOreRecipe recipe) {
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
            ItemStack referenceCapsule = configuredOutput;
            for (int i = 0; i < invC.getHeight(); ++i) {
                for (int j = 0; j < invC.getWidth(); ++j) {
                    ItemStack itemstack = invC.getStackInRowAndColumn(j, i);

                    if (CapsuleItem.isLinkedStateCapsule(itemstack) || CapsuleItem.isReward(itemstack)) {
                        // This blueprint will take the source structure name by copying it here
                        // a new dedicated template is created later.
                        // @see CapsuleItem.onCreated
                        referenceCapsule = itemstack;
                    }
                }
            }
            try {
                ItemStack blueprintItem = Capsule.newLinkedCapsuleItemStack(
                        CapsuleItem.getStructureName(referenceCapsule),
                        CapsuleItem.getBaseColor(recipe.getRecipeOutput()),
                        0xFFFFFF,
                        CapsuleItem.getSize(referenceCapsule),
                        CapsuleItem.isOverpowered(referenceCapsule),
                        referenceCapsule.getTagCompound() != null ? referenceCapsule.getTagCompound().getString("label") : null,
                        0
                );
                CapsuleItem.setBlueprint(blueprintItem);
                // hack to force a tempalte copy if it's not done after craft
                if (blueprintItem.getTagCompound() != null) {
                    blueprintItem.getTagCompound().setBoolean("templateShouldBeCopied", true);
                }
                CapsuleItem.setState(blueprintItem, STATE_DEPLOYED);
                return blueprintItem;
            } catch (Exception e) {
                e.printStackTrace();
                return ItemStack.EMPTY;
            }
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