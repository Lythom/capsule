package capsule.recipes;


import capsule.Config;
import capsule.items.CapsuleItem;
import capsule.items.CapsuleItems;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

import static capsule.items.CapsuleItem.CapsuleState.EMPTY;

public class UpgradeCapsuleRecipe implements CraftingRecipe {
    /**
     * Is the ItemStack that you repair.
     */
    public final Ingredient upgradeIngredient;

    public UpgradeCapsuleRecipe(Ingredient upgradeIngredient) {
        this.upgradeIngredient = upgradeIngredient;
    }

    public ItemStack getResultItem(HolderLookup.Provider registryAccess) {
        return CapsuleItems.getUpgradedCapsule(CapsuleItems.withState(EMPTY), 1);
    }

    /**
     * Used to check if a recipe matches current crafting inventory
     */
    public boolean matches(CraftingInput invC, Level worldIn) {

        ItemStack sourceCapsule = ItemStack.EMPTY;
        int material = 0;
        for (int i = 0; i < invC.size(); ++i) {
            ItemStack itemstack = invC.getItem(i);

            if (!itemstack.isEmpty()
                    && itemstack.getItem() instanceof CapsuleItem
                    && CapsuleItem.hasState(itemstack, EMPTY)
                    && CapsuleItem.getUpgradeLevel(itemstack) < Config.upgradeLimit) {
                sourceCapsule = itemstack;
            } else if (upgradeIngredient.test(itemstack)) {
                material++;
            } else if (!itemstack.isEmpty()) {
                return false;
            }

        }

        return sourceCapsule != ItemStack.EMPTY && material > 0 && CapsuleItem.getUpgradeLevel(sourceCapsule) + material <= Config.upgradeLimit;
    }

    /**
     * Returns an Item that is the result of this recipe
     */
    public ItemStack assemble(CraftingInput invC, HolderLookup.Provider registryAccess) {
        ItemStack input = ItemStack.EMPTY;
        int material = 0;
        for (int i = 0; i < invC.size(); ++i) {
            ItemStack itemstack = invC.getItem(i);

            if (!itemstack.isEmpty()
                    && itemstack.getItem() instanceof CapsuleItem
                    && CapsuleItem.hasState(itemstack, EMPTY)
                    && CapsuleItem.getUpgradeLevel(itemstack) < Config.upgradeLimit) {
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
        if (newUpgraded > Config.upgradeLimit) newUpgraded = Config.upgradeLimit;

        CapsuleItem.setSize(copy, newSize);
        CapsuleItem.setUpgradeLevel(copy, newUpgraded);

        return copy;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return CapsuleRecipes.UPGRADE_CAPSULE_SERIALIZER.get();
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public CraftingBookCategory category() {
        return CraftingBookCategory.MISC;
    }

    public static class Serializer implements RecipeSerializer<UpgradeCapsuleRecipe> {
        public static final MapCodec<UpgradeCapsuleRecipe> CODEC = RecordCodecBuilder.mapCodec(
                instance -> instance.group(
                                Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(recipe -> recipe.upgradeIngredient)
                        )
                        .apply(instance, UpgradeCapsuleRecipe::new)
        );

        public static final StreamCodec<RegistryFriendlyByteBuf, UpgradeCapsuleRecipe> STREAM_CODEC =
                StreamCodec.composite(
                        Ingredient.CONTENTS_STREAM_CODEC, recipe -> recipe.upgradeIngredient,
                        UpgradeCapsuleRecipe::new
                );

        @Override
        public MapCodec<UpgradeCapsuleRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, UpgradeCapsuleRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}