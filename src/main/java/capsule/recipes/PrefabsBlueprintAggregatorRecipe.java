package capsule.recipes;

import capsule.Config;
import capsule.StructureSaver;
import capsule.helpers.Blueprint;
import capsule.items.CapsuleItem;
import capsule.items.CapsuleItems;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.CommonHooks;
import org.apache.commons.lang3.tuple.Triple;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static capsule.items.CapsuleItem.CapsuleState.BLUEPRINT;

public class PrefabsBlueprintAggregatorRecipe extends CustomRecipe {

    public static PrefabsBlueprintAggregatorRecipe instance;

    public List<PrefabsBlueprintAggregatorRecipe.PrefabsBlueprintCapsuleRecipe> recipes = new ArrayList<>();

    public PrefabsBlueprintAggregatorRecipe() {
        super(CraftingBookCategory.MISC);
        instance = this;
    }

    /**
     * Must be called
     * > after server start (providing a server is required) and
     * < before RecipesUpdatedEvent (so that the recupies are registered by JEI)
     *
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
        return CapsuleRecipes.PREFABS_AGGREGATOR_SERIALIZER.get();
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
    public ItemStack assemble(CraftingContainer inv, RegistryAccess registryAccess) {
        Optional<PrefabsBlueprintCapsuleRecipe> recipe = recipes.stream().filter(r -> r.matches(inv)).findFirst();
        if (recipe.isPresent()) return recipe.get().assemble(inv, registryAccess);
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    public ItemStack getResultItem(RegistryAccess registryAccess) {
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
            FixIngredient(template, ingredients.getLeft(), "1");
            FixIngredient(template, ingredients.getMiddle(), "2");
            FixIngredient(template, ingredients.getRight(), "3");
            this.recipe = ShapedRecipe.Serializer.SHAPED_RECIPE.codec().parse(JsonOps.INSTANCE, template).getOrThrow(false, null);
//            Optional<RecipeHolder<?>> holder = RecipeManager.fromJson(id, template, JsonOps.INSTANCE);
//	        this.recipe = holder.map(recipeHolder -> (ShapedRecipe) recipeHolder.value()).orElse(null);
        }

        private void FixIngredient(JsonObject template, StructureSaver.ItemStackKey ingredientKey, String key) {
            if (ingredientKey != null) {
                // Add the key for the item
                var keyOne = new JsonObject();
                keyOne.addProperty("item", BuiltInRegistries.ITEM.getKey(ingredientKey.itemStack.getItem()).toString());
                template.getAsJsonObject("key").add(key, keyOne);
            } else {
                // remove the pattern entry if no ingredient by replacing the key with an empty space
                JsonArray patternArr = GsonHelper.getAsJsonArray(template, "pattern");
                if (patternArr.size() != 3) {
                    throw new JsonSyntaxException("pattern entry in prefab_blueprint_recipe.json should define a 3x3 recipe.");
                }
                for (int i = 0; i < 3; i++) {
                    patternArr.set(i, new JsonPrimitive(patternArr.get(i).getAsString().replaceAll(key, " ")));
                }
            }
        }

        public PrefabsBlueprintCapsuleRecipe(ResourceLocation id, ShapedRecipe serializedRecipe) {
            this.id = id;
            this.recipe = serializedRecipe;
        }

        public ItemStack getResultItem(RegistryAccess registryAccess) {
            return recipe.getResultItem(registryAccess);
        }

        /**
         * Only blueprint material is consumed. Materials used inside blueprint are given back.
         */
        public NonNullList<ItemStack> getRemainingItems(CraftingContainer inv) {
            NonNullList<ItemStack> nonnulllist = NonNullList.withSize(inv.getContainerSize(), ItemStack.EMPTY);

            for (int i = 0; i < nonnulllist.size(); ++i) {
                ItemStack itemstack = inv.getItem(i);
                nonnulllist.set(i, CommonHooks.getCraftingRemainingItem(itemstack));
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

        public ItemStack assemble(CraftingContainer invC, RegistryAccess registryAccess) {
            return recipe.assemble(invC, registryAccess);
        }

        @Override
        public boolean canCraftInDimensions(int width, int height) {
            return recipe.canCraftInDimensions(width, height);
        }

        @Override
        public CraftingBookCategory category() {
            return CraftingBookCategory.MISC;
        }
    }


    public static class Serializer implements RecipeSerializer<PrefabsBlueprintAggregatorRecipe> {

        private static final Codec<PrefabsBlueprintAggregatorRecipe> CODEC =
                MapCodec.unit(() -> instance != null ? instance : new PrefabsBlueprintAggregatorRecipe()).stable().codec();

        @Override
        public Codec<PrefabsBlueprintAggregatorRecipe> codec() {
            return CODEC;
        }

        @Override
        public PrefabsBlueprintAggregatorRecipe fromNetwork(FriendlyByteBuf buffer) {
            if (instance == null) {
                instance = new PrefabsBlueprintAggregatorRecipe();
            }
            instance.recipes.clear();

            int size = buffer.readInt();
            for (int i = 0; i < size; i++) {
                ResourceLocation id = ResourceLocation.parse(buffer.readUtf());
                ShapedRecipe recipe = ShapedRecipe.Serializer.SHAPED_RECIPE.fromNetwork(buffer);
                instance.recipes.add(new PrefabsBlueprintCapsuleRecipe(id, recipe));
            }

            return instance;
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, PrefabsBlueprintAggregatorRecipe recipe) {
            buffer.writeInt(recipe.recipes.size());
            for (PrefabsBlueprintCapsuleRecipe subRecipe : recipe.recipes) {
                buffer.writeUtf(subRecipe.id.toString());
                ShapedRecipe.Serializer.SHAPED_RECIPE.toNetwork(buffer, subRecipe.recipe);
            }
        }
    }
}