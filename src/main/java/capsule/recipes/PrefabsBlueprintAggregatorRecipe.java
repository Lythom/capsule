package capsule.recipes;

import capsule.Config;
import capsule.StructureSaver;
import capsule.helpers.Blueprint;
import capsule.items.CapsuleItem;
import capsule.items.CapsuleItems;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.item.crafting.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.GsonHelper;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.apache.commons.lang3.tuple.Triple;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static capsule.items.CapsuleItem.CapsuleState.BLUEPRINT;

import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;

public class PrefabsBlueprintAggregatorRecipe extends CustomRecipe {

    public static PrefabsBlueprintAggregatorRecipe instance;

    public List<PrefabsBlueprintAggregatorRecipe.PrefabsBlueprintCapsuleRecipe> recipes = new ArrayList<>();

    public PrefabsBlueprintAggregatorRecipe(ResourceLocation idIn) {
        super(idIn);
        instance = this;
    }

    /**
     * Must be called
     * > after server start (providing a server is required) and
     * < before RecipesUpdatedEvent (so that the recupies are registered by JEI)
     * @param resourceManager
     */
    public void populateRecipes(ResourceManager resourceManager) {
        if (resourceManager == null) return;
        List<String> prefabsTemplatesList = Config.prefabsTemplatesList;
        recipes.clear();
        Blueprint.createDynamicPrefabRecipes(
                resourceManager,
                prefabsTemplatesList,
                (id, recipe, ingredients) -> recipes.add(
                        new PrefabsBlueprintAggregatorRecipe.PrefabsBlueprintCapsuleRecipe(id, recipe, ingredients)
                )
        );
    }


    @Override
    public RecipeSerializer<?> getSerializer() {
        return CapsuleRecipes.PREFABS_AGGREGATOR_SERIALIZER;
    }

    /**
     * Used to check if a recipe matches current crafting inventory
     */
    public boolean matches(CraftingContainer inv, Level worldIn) {
        return recipes.stream().anyMatch(r -> r.matches(inv));
    }

    /**
     * Returns an Item that is the result of this recipe
     */
    public ItemStack assemble(CraftingContainer inv) {
        Optional<PrefabsBlueprintCapsuleRecipe> recipe = recipes.stream().filter(r -> r.matches(inv)).findFirst();
        if (recipe.isPresent()) return recipe.get().assemble(inv);
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    public ItemStack getResultItem() {
        return CapsuleItems.withState(BLUEPRINT);
    }

    public NonNullList<ItemStack> getRemainingItems(CraftingContainer inv) {
        Optional<PrefabsBlueprintCapsuleRecipe> recipe = recipes.stream().filter(r -> r.matches(inv)).findFirst();
        if (recipe.isPresent()) return recipe.get().getRemainingItems(inv);
        return NonNullList.withSize(inv.getContainerSize(), ItemStack.EMPTY);
    }

    public static class PrefabsBlueprintCapsuleRecipe implements CraftingRecipe {
        private final ResourceLocation id;

        public final ShapedRecipe recipe;
        private int ingredientOneIndex = 4;
        private int ingredientTwoIndex = 0;
        private int ingredientThreeIndex = 2;

        public PrefabsBlueprintCapsuleRecipe(ResourceLocation id, JsonObject template, Triple<StructureSaver.ItemStackKey, StructureSaver.ItemStackKey, StructureSaver.ItemStackKey> ingredients) {
            this.id = id;
            this.recipe = ShapedRecipe.Serializer.SHAPED_RECIPE.fromJson(id, template);
            buildRecipeFromPattern(template, ingredients);
        }

        public PrefabsBlueprintCapsuleRecipe(ResourceLocation id, ShapedRecipe serializedRecipe) {
            this.id = id;
            this.recipe = serializedRecipe;
        }

        public void buildRecipeFromPattern(JsonObject template, Triple<StructureSaver.ItemStackKey, StructureSaver.ItemStackKey, StructureSaver.ItemStackKey> ingredients) {
            JsonArray patternArr = GsonHelper.getAsJsonArray(template, "pattern");
            String pattern = patternArr.get(0).getAsString() + patternArr.get(1).getAsString() + patternArr.get(2).getAsString();
            ingredientOneIndex = pattern.indexOf("1");
            ingredientTwoIndex = pattern.indexOf("2");
            ingredientThreeIndex = pattern.indexOf("3");
            this.recipe.getIngredients().set(ingredientOneIndex, Ingredient.of(ingredients.getLeft().itemStack));
            if (ingredients.getMiddle() != null) {
                this.recipe.getIngredients().set(ingredientTwoIndex, Ingredient.of(ingredients.getMiddle().itemStack));
            } else {
                this.recipe.getIngredients().set(ingredientTwoIndex, Ingredient.EMPTY);
            }
            if (ingredients.getRight() != null) {
                this.recipe.getIngredients().set(ingredientThreeIndex, Ingredient.of(ingredients.getRight().itemStack));
            } else {
                this.recipe.getIngredients().set(ingredientThreeIndex, Ingredient.EMPTY);
            }
        }

        public ItemStack getResultItem() {
            return recipe.getResultItem();
        }

        /**
         * Only blueprint material is consumed. Materials used inside blueprint are given back.
         */
        public NonNullList<ItemStack> getRemainingItems(CraftingContainer inv) {
            NonNullList<ItemStack> nonnulllist = NonNullList.withSize(inv.getContainerSize(), ItemStack.EMPTY);

            for (int i = 0; i < nonnulllist.size(); ++i) {
                ItemStack itemstack = inv.getItem(i);
                nonnulllist.set(i, net.minecraftforge.common.ForgeHooks.getContainerItem(itemstack));
                if (itemstack.getItem() instanceof CapsuleItem) {
                    nonnulllist.set(i, itemstack.copy());
                } else if (i == ingredientOneIndex || i == ingredientTwoIndex || i == ingredientThreeIndex) {
                    ItemStack refund = itemstack.copy();
                    refund.setCount(1);
                    nonnulllist.set(i, refund);
                }
            }

            return nonnulllist;
        }

        @Override
        public ResourceLocation getId() {
            return id;
        }

        @Override
        public RecipeSerializer<?> getSerializer() {
            return ShapedRecipe.Serializer.SHAPED_RECIPE;
        }

        public boolean matches(CraftingContainer inv) {
            for (int i = 0; i <= inv.getWidth() - recipe.getWidth(); ++i) {
                for (int j = 0; j <= inv.getHeight() - recipe.getHeight(); ++j) {
                    if (this.checkMatch(inv, i, j, true)) {
                        return true;
                    }

                    if (this.checkMatch(inv, i, j, false)) {
                        return true;
                    }
                }
            }

            return false;
        }

        public boolean matches(CraftingContainer inv, Level worldIn) {
            return matches(inv);
        }

        /**
         * Checks if the region of a crafting inventory is match for the recipe.
         */
        private boolean checkMatch(CraftingContainer craftingInventory, int p_77573_2_, int p_77573_3_, boolean p_77573_4_) {
            for (int i = 0; i < craftingInventory.getWidth(); ++i) {
                for (int j = 0; j < craftingInventory.getHeight(); ++j) {
                    int k = i - p_77573_2_;
                    int l = j - p_77573_3_;
                    Ingredient ingredient = Ingredient.EMPTY;
                    if (k >= 0 && l >= 0 && k < recipe.getWidth() && l < recipe.getHeight()) {
                        if (p_77573_4_) {
                            ingredient = recipe.getIngredients().get(recipe.getWidth() - k - 1 + l * this.recipe.getWidth());
                        } else {
                            ingredient = recipe.getIngredients().get(k + l * recipe.getWidth());
                        }
                    }

                    if (!ingredient.test(craftingInventory.getItem(i + j * craftingInventory.getWidth()))) {
                        return false;
                    }
                }
            }

            return true;
        }

        public ItemStack assemble(CraftingContainer invC) {
            return recipe.assemble(invC);
        }

        @Override
        public boolean canCraftInDimensions(int width, int height) {
            return recipe.canCraftInDimensions(width, height);
        }
    }


    public static class Serializer extends net.minecraftforge.registries.ForgeRegistryEntry<RecipeSerializer<?>> implements RecipeSerializer<PrefabsBlueprintAggregatorRecipe> {

        @Override
        public PrefabsBlueprintAggregatorRecipe fromJson(ResourceLocation recipeId, JsonObject json) {
            return instance != null ? instance : new PrefabsBlueprintAggregatorRecipe(recipeId);
        }

        @Override
        public PrefabsBlueprintAggregatorRecipe fromNetwork(ResourceLocation recipeId, FriendlyByteBuf buffer) {
            if (instance == null) {
                instance = new PrefabsBlueprintAggregatorRecipe(recipeId);
            }
            instance.recipes.clear();

            RecipeSerializer<ShapedRecipe> serializer = ShapedRecipe.Serializer.SHAPED_RECIPE;
            int size = buffer.readInt();
            for (int i = 0; i < size; i++) {
                ResourceLocation id = new ResourceLocation(buffer.readUtf());
                ShapedRecipe recipe = serializer.fromNetwork(id, buffer);
                instance.recipes.add(new PrefabsBlueprintCapsuleRecipe(id, recipe));
            }

            return instance;
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, PrefabsBlueprintAggregatorRecipe recipe) {
            RecipeSerializer<ShapedRecipe> serializer = ShapedRecipe.Serializer.SHAPED_RECIPE;
            buffer.writeInt(recipe.recipes.size());
            for (PrefabsBlueprintCapsuleRecipe subRecipe : recipe.recipes) {
                buffer.writeUtf(subRecipe.id.toString());
                serializer.toNetwork(buffer, subRecipe.recipe);
            }
        }
    }
}