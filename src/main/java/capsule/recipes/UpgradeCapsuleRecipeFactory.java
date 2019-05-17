package capsule.recipes;

import capsule.Config;
import capsule.items.CapsuleItem;
import capsule.items.CapsuleItems;
import com.google.gson.JsonObject;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.world.World;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.common.crafting.IRecipeFactory;
import net.minecraftforge.common.crafting.JsonContext;

public class UpgradeCapsuleRecipeFactory implements IRecipeFactory {

    @Override
    public IRecipe parse(JsonContext context, JsonObject json) {
        Ingredient ing = CraftingHelper.getIngredient(json.getAsJsonObject("ingredients"), context);
        return new UpgradeCapsuleRecipe(ing.getMatchingStacks()[0].getItem());
    }

    class UpgradeCapsuleRecipe extends net.minecraftforge.registries.IForgeRegistryEntry.Impl<IRecipe> implements IRecipe {
        /**
         * Is the ItemStack that you repair.
         */
        private final Item upgradeItem;

        public UpgradeCapsuleRecipe(Item upgradeItem) {
            this.upgradeItem = upgradeItem;
        }

        public ItemStack getRecipeOutput() {
            return ItemStack.EMPTY;
        }

        /**
         * Used to check if a recipe matches current crafting inventory
         */
        public boolean matches(InventoryCrafting craftingGrid, World worldIn) {

            ItemStack sourceCapsule = ItemStack.EMPTY;
            int material = 0;
            for (int i = 0; i < craftingGrid.getHeight(); ++i) {
                for (int j = 0; j < craftingGrid.getWidth(); ++j) {
                    ItemStack itemstack = craftingGrid.getStackInRowAndColumn(j, i);

                    if (!itemstack.isEmpty() && itemstack.getItem() == CapsuleItems.capsule && itemstack.getItemDamage() == CapsuleItem.STATE_EMPTY && CapsuleItem.getUpgradeLevel(itemstack) < Config.upgradeLimit) {
                        sourceCapsule = itemstack;
                    } else if (!itemstack.isEmpty() && itemstack.getItem() == upgradeItem) {
                        material++;
                    } else if (!itemstack.isEmpty()) {
                        return false;
                    }
                }
            }

            return sourceCapsule != null && material > 0 && CapsuleItem.getUpgradeLevel(sourceCapsule) + material <= Config.upgradeLimit;
        }

        /**
         * Returns an Item that is the result of this recipe
         */
        public ItemStack getCraftingResult(InventoryCrafting craftingGrid) {

            ItemStack input = ItemStack.EMPTY;
            int material = 0;
            for (int i = 0; i < craftingGrid.getHeight(); ++i) {
                for (int j = 0; j < craftingGrid.getWidth(); ++j) {
                    ItemStack itemstack = craftingGrid.getStackInRowAndColumn(j, i);

                    if (!itemstack.isEmpty() && itemstack.getItem() == CapsuleItems.capsule && itemstack.getItemDamage() == CapsuleItem.STATE_EMPTY && CapsuleItem.getUpgradeLevel(itemstack) < Config.upgradeLimit) {
                        input = itemstack;
                    } else if (!itemstack.isEmpty() && itemstack.getItem() == upgradeItem) {
                        material++;
                    } else if (!itemstack.isEmpty()) {
                        return ItemStack.EMPTY;
                    }
                }
            }

            if (input == null) return ItemStack.EMPTY;

            ItemStack copy = input.copy();
            int newSize = CapsuleItem.getSize(input) + material * 2;
            int newUpgraded = CapsuleItem.getUpgradeLevel(input) + material;

            if (newSize > CapsuleItem.CAPSULE_MAX_CAPTURE_SIZE) newSize = CapsuleItem.CAPSULE_MAX_CAPTURE_SIZE;
            if (newUpgraded > Config.upgradeLimit) newUpgraded = Config.upgradeLimit;

            CapsuleItem.setSize(copy, newSize);
            CapsuleItem.setUpgradeLevel(copy, newUpgraded);

            return copy;
        }

        @Override
        public boolean canFit(int width, int height) {
            return width * height >= 2;
        }

        public boolean isDynamic() {
            return true;
        }
    }
}