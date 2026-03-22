package capsule.recipes;

import capsule.items.CapsuleItem;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.NonNullList;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.CommonHooks;

public class RecoveryCapsuleRecipe extends ShapelessRecipe {
    final String group;
    final CraftingBookCategory category;
    final ItemStack result;
    final NonNullList<Ingredient> ingredients;

    public RecoveryCapsuleRecipe(String group, CraftingBookCategory category, ItemStack result, NonNullList<Ingredient> ingredients) {
        super(group, category, result, ingredients);
        this.group = group;
        this.category = category;
        this.result = result;
        this.ingredients = ingredients;
    }

    public ItemStack getResultItem(HolderLookup.Provider registryAccess) {
        return super.getResultItem(registryAccess);
    }

    /**
     * The original capsule remains in the crafting grid
     */
    public NonNullList<ItemStack> getRemainingItems(CraftingInput inv) {
        NonNullList<ItemStack> nonnulllist = NonNullList.withSize(inv.size(), ItemStack.EMPTY);

        for (int i = 0; i < nonnulllist.size(); ++i) {
            ItemStack itemstack = inv.getItem(i);
            nonnulllist.set(i, CommonHooks.getCraftingRemainingItem(itemstack));
            if (itemstack.getItem() instanceof CapsuleItem) {
                nonnulllist.set(i, itemstack.copy());
            }
        }

        return nonnulllist;
    }

    /**
     * Used to check if a recipe matches current crafting inventory
     */
    public boolean matches(CraftingInput inv, Level level) {
        return super.matches(inv, level);
    }

    /**
     * Returns a copy built from the original capsule.
     */
    public ItemStack assemble(CraftingInput invC, HolderLookup.Provider registryAccess) {
        for (int i = 0; i < invC.size(); ++i) {
            ItemStack itemstack = invC.getItem(i);

            if (CapsuleItem.isLinkedStateCapsule(itemstack)) {
                ItemStack copy = itemstack.copy();
                CapsuleItem.setOneUse(copy);
                return copy;
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return super.canCraftInDimensions(width, height);
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return CapsuleRecipes.RECOVERY_CAPSULE_SERIALIZER.get();
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public CraftingBookCategory category() {
        return CraftingBookCategory.MISC;
    }

    public static class Serializer implements RecipeSerializer<RecoveryCapsuleRecipe> {

        private static final MapCodec<RecoveryCapsuleRecipe> CODEC = RecordCodecBuilder.mapCodec(
                instance -> instance.group(
                                Codec.STRING.optionalFieldOf("group", "").forGetter(p_301127_ -> p_301127_.group),
                                CraftingBookCategory.CODEC.fieldOf("category").orElse(CraftingBookCategory.MISC).forGetter(p_301133_ -> p_301133_.category),
                                ItemStack.STRICT_CODEC.fieldOf("result").forGetter(p_301142_ -> p_301142_.result),
                                Ingredient.CODEC_NONEMPTY
                                        .listOf()
                                        .fieldOf("ingredients")
                                        .flatXmap(
                                                p_301021_ -> {
                                                    Ingredient[] aingredient = p_301021_
                                                            .toArray(Ingredient[]::new);
                                                    if (aingredient.length == 0) {
                                                        return DataResult.error(() -> "No ingredients for shapeless recipe");
                                                    } else {
                                                        return aingredient.length > ShapedRecipePattern.getMaxHeight() * ShapedRecipePattern.getMaxWidth()
                                                                ? DataResult.error(() -> "Too many ingredients for shapeless recipe. The maximum is: %s".formatted(ShapedRecipePattern.getMaxHeight() * ShapedRecipePattern.getMaxWidth()))
                                                                : DataResult.success(NonNullList.of(Ingredient.EMPTY, aingredient));
                                                    }
                                                },
                                                DataResult::success
                                        )
                                        .forGetter(p_300975_ -> p_300975_.ingredients)
                        )
                        .apply(instance, RecoveryCapsuleRecipe::new)
        );

        private static final StreamCodec<RegistryFriendlyByteBuf, RecoveryCapsuleRecipe> STREAM_CODEC =
                StreamCodec.of(Serializer::toNetwork, Serializer::fromNetwork);

        private static RecoveryCapsuleRecipe fromNetwork(RegistryFriendlyByteBuf buffer) {
            String s = buffer.readUtf();
            CraftingBookCategory category = buffer.readEnum(CraftingBookCategory.class);
            int i = buffer.readVarInt();
            NonNullList<Ingredient> ingredients = NonNullList.withSize(i, Ingredient.EMPTY);

            for (int j = 0; j < ingredients.size(); ++j) {
                ingredients.set(j, Ingredient.CONTENTS_STREAM_CODEC.decode(buffer));
            }

            ItemStack result = ItemStack.STREAM_CODEC.decode(buffer);
            return new RecoveryCapsuleRecipe(s, category, result, ingredients);
        }

        private static void toNetwork(RegistryFriendlyByteBuf buffer, RecoveryCapsuleRecipe recipe) {
            buffer.writeUtf(recipe.group);
            buffer.writeEnum(recipe.category);
            buffer.writeVarInt(recipe.ingredients.size());

            for (Ingredient ingredient : recipe.ingredients) {
                Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, ingredient);
            }

            ItemStack.STREAM_CODEC.encode(buffer, recipe.result);
        }

        @Override
        public MapCodec<RecoveryCapsuleRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, RecoveryCapsuleRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}