package capsule.recipes;


import capsule.CommonProxy;
import capsule.Config;
import capsule.items.CapsuleItem;
import capsule.items.CapsuleItems;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.ICraftingRecipe;
import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.JSONUtils;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

public class UpgradeCapsuleRecipe implements ICraftingRecipe {
    /**
     * Is the ItemStack that you repair.
     */
    public final Ingredient upgradeIngredient;
    private final ResourceLocation recipeId;

    public UpgradeCapsuleRecipe(ResourceLocation recipeId, Ingredient upgradeIngredient) {
        this.upgradeIngredient = upgradeIngredient;
        this.recipeId = recipeId;
    }

    public ItemStack getRecipeOutput() {
        return CapsuleItems.getUpgradedCapsule(CapsuleItems.withState(CapsuleItem.STATE_EMPTY), 1);
    }

    /**
     * Used to check if a recipe matches current crafting inventory
     */
    public boolean matches(CraftingInventory invC, World worldIn) {

        ItemStack sourceCapsule = ItemStack.EMPTY;
        int material = 0;
        for (int i = 0; i < invC.getSizeInventory(); ++i) {
            ItemStack itemstack = invC.getStackInSlot(i);

            if (!itemstack.isEmpty()
                    && itemstack.getItem() instanceof CapsuleItem
                    && itemstack.getDamage() == CapsuleItem.STATE_EMPTY
                    && CapsuleItem.getUpgradeLevel(itemstack) < Config.upgradeLimit.get()) {
                sourceCapsule = itemstack;
            } else if (upgradeIngredient.test(itemstack)) {
                material++;
            } else if (!itemstack.isEmpty()) {
                return false;
            }

        }

        return sourceCapsule != ItemStack.EMPTY && material > 0 && CapsuleItem.getUpgradeLevel(sourceCapsule) + material <= Config.upgradeLimit.get();
    }

    /**
     * Returns an Item that is the result of this recipe
     */
    public ItemStack getCraftingResult(CraftingInventory invC) {
        ItemStack input = ItemStack.EMPTY;
        int material = 0;
        for (int i = 0; i < invC.getSizeInventory(); ++i) {
            ItemStack itemstack = invC.getStackInSlot(i);

            if (!itemstack.isEmpty()
                    && itemstack.getItem() instanceof CapsuleItem
                    && itemstack.getDamage() == CapsuleItem.STATE_EMPTY
                    && CapsuleItem.getUpgradeLevel(itemstack) < Config.upgradeLimit.get()) {
                input = itemstack;
            } else if (upgradeIngredient.test(itemstack)) {
                material++;
            } else if (!itemstack.isEmpty()) {
                return ItemStack.EMPTY;
            }

        }

        if (input == ItemStack.EMPTY) return ItemStack.EMPTY;

        ItemStack copy = input.copy();
        int newSize = CapsuleItem.getSize(input) + material * 2;
        int newUpgraded = CapsuleItem.getUpgradeLevel(input) + material;

        if (newSize > CapsuleItem.CAPSULE_MAX_CAPTURE_SIZE) newSize = CapsuleItem.CAPSULE_MAX_CAPTURE_SIZE;
        if (newUpgraded > Config.upgradeLimit.get()) newUpgraded = Config.upgradeLimit.get();

        CapsuleItem.setSize(copy, newSize);
        CapsuleItem.setUpgradeLevel(copy, newUpgraded);

        return copy;
    }

    @Override
    public boolean canFit(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public IRecipeSerializer<?> getSerializer() {
        return CommonProxy.UPGRADE_CAPSULE_SERIALIZER;
    }

    @Override
    public boolean isDynamic() {
        return true;
    }

    @Override
    public ResourceLocation getId() {
        return recipeId;
    }

    public static class Serializer extends net.minecraftforge.registries.ForgeRegistryEntry<IRecipeSerializer<?>> implements IRecipeSerializer<UpgradeCapsuleRecipe> {

        private static NonNullList<Ingredient> readIngredients(JsonArray p_199568_0_) {
            NonNullList<Ingredient> nonnulllist = NonNullList.create();

            for (int i = 0; i < p_199568_0_.size(); ++i) {
                Ingredient ingredient = Ingredient.deserialize(p_199568_0_.get(i));
                if (!ingredient.hasNoMatchingItems()) {
                    nonnulllist.add(ingredient);
                }
            }

            return nonnulllist;
        }

        @Override
        public UpgradeCapsuleRecipe read(ResourceLocation recipeId, JsonObject json) {
            NonNullList<Ingredient> nonnulllist = readIngredients(JSONUtils.getJsonArray(json, "ingredients"));
            if (nonnulllist.isEmpty()) {
                throw new JsonParseException("No ingredients for shapeless recipe");
            } else {
                return new UpgradeCapsuleRecipe(recipeId, nonnulllist.get(0));
            }
        }

        @Override
        public UpgradeCapsuleRecipe read(ResourceLocation recipeId, PacketBuffer buffer) {
            return new UpgradeCapsuleRecipe(recipeId, Ingredient.read(buffer));
        }

        @Override
        public void write(PacketBuffer buffer, UpgradeCapsuleRecipe recipe) {
            recipe.upgradeIngredient.write(buffer);
        }
    }
}