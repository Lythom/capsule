package capsule.recipes;

import capsule.Config;
import capsule.StructureSaver;
import capsule.helpers.Blueprint;
import capsule.items.CapsuleItem;
import capsule.items.CapsuleItems;
import capsule.platform.Services;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;
import org.apache.commons.lang3.tuple.Triple;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import static capsule.items.CapsuleItem.CapsuleState.BLUEPRINT;

/**
 * One recipe (data/capsule/recipe/aggregate_all_prefabs.json) standing for all the prefab blueprint recipes, which are
 * generated from config/capsule/prefab_blueprint_recipe.json and the prefab templates when the server starts.
 * <p>
 * The client needs the prefab recipes too (creative tab, recipe viewers): the loaders send it the crafting recipes
 * (NeoForge OnDatapackSyncEvent#sendRecipes, Fabric RecipeSynchronization), and this recipe is written to the network
 * with its prefab recipes (STREAM_CODEC). A client connected to a dedicated server reads them into its own instance.
 */
public class PrefabsBlueprintAggregatorRecipe extends CustomRecipe {

    /**
     * The server's recipe, which populateRecipes fills. A datapack reload reads aggregate_all_prefabs.json again and
     * gets this same instance back, prefab recipes included.
     */
    public static PrefabsBlueprintAggregatorRecipe instance;

    public static final MapCodec<PrefabsBlueprintAggregatorRecipe> MAP_CODEC =
            MapCodec.unit(() -> instance != null ? instance : new PrefabsBlueprintAggregatorRecipe());
    public static final StreamCodec<RegistryFriendlyByteBuf, PrefabsBlueprintAggregatorRecipe> STREAM_CODEC = StreamCodec.composite(
            PrefabsBlueprintCapsuleRecipe.STREAM_CODEC.apply(ByteBufCodecs.list()), recipe -> recipe.recipes,
            PrefabsBlueprintAggregatorRecipe::new);
    public static final RecipeSerializer<PrefabsBlueprintAggregatorRecipe> SERIALIZER = new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    public final List<PrefabsBlueprintAggregatorRecipe.PrefabsBlueprintCapsuleRecipe> recipes;

    public PrefabsBlueprintAggregatorRecipe() {
        this.recipes = new ArrayList<>();
        instance = this;
    }

    /**
     * The recipe as received from the server.
     */
    private PrefabsBlueprintAggregatorRecipe(List<PrefabsBlueprintCapsuleRecipe> recipes) {
        this.recipes = new ArrayList<>(recipes);
    }

    /**
     * Must be called after server start (the templates and the config are read from the server) and before the
     * recipes are sent to the players (so that the clients and their recipe viewers get the prefab recipes).
     * A prefab_blueprint_recipe.json that cannot be used is logged, and then there is no prefab recipe.
     */
    public void populateRecipes(ResourceManager resourceManager, HolderLookup.Provider registries) {
        if (resourceManager == null) return;
        List<String> prefabsTemplatesList = Config.prefabsTemplatesList;
        List<PrefabsBlueprintCapsuleRecipe> created = new ArrayList<>();
        boolean valid = Blueprint.createDynamicPrefabRecipes(
                resourceManager,
                prefabsTemplatesList,
                (id, recipe, ingredients) -> created.add(
                        new PrefabsBlueprintAggregatorRecipe.PrefabsBlueprintCapsuleRecipe(id, recipe, ingredients, registries)
                )
        );
        recipes.clear();
        if (valid) recipes.addAll(created);
    }


    @Override
    public RecipeSerializer<PrefabsBlueprintAggregatorRecipe> getSerializer() {
        return CapsuleRecipes.PREFABS_AGGREGATOR_SERIALIZER.get();
    }

    /**
     * Used to check if a recipe matches current crafting inventory
     */
    @Override
    public boolean matches(CraftingInput inv, Level worldIn) {
        return recipes.stream().anyMatch(r -> r.matches(inv));
    }

    /**
     * Returns an Item that is the result of this recipe
     */
    @Override
    public ItemStack assemble(CraftingInput inv) {
        Optional<PrefabsBlueprintCapsuleRecipe> recipe = recipes.stream().filter(r -> r.matches(inv)).findFirst();
        if (recipe.isPresent()) return recipe.get().assemble(inv);
        return ItemStack.EMPTY;
    }

    public ItemStack getResultItem() {
        return CapsuleItems.withState(BLUEPRINT);
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput inv) {
        Optional<PrefabsBlueprintCapsuleRecipe> recipe = recipes.stream().filter(r -> r.matches(inv)).findFirst();
        if (recipe.isPresent()) return recipe.get().getRemainingItems(inv);
        return NonNullList.withSize(inv.size(), ItemStack.EMPTY);
    }

    public static class PrefabsBlueprintCapsuleRecipe implements CraftingRecipe {
        public static final StreamCodec<RegistryFriendlyByteBuf, PrefabsBlueprintCapsuleRecipe> STREAM_CODEC = StreamCodec.composite(
                Identifier.STREAM_CODEC, recipe -> recipe.id,
                ShapedRecipe.STREAM_CODEC, recipe -> recipe.recipe,
                ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list()), recipe -> recipe.templateIngredientIndexes,
                PrefabsBlueprintCapsuleRecipe::new);

        private final Identifier id;

        public final ShapedRecipe recipe;
        /**
         * Indexes in the recipe pattern of the template ingredients 1, 2 and 3, given back after crafting.
         */
        private final List<Integer> templateIngredientIndexes;

        /**
         * @throws JsonParseException when the template (prefab_blueprint_recipe.json) is not a valid shaped recipe
         */
        public PrefabsBlueprintCapsuleRecipe(Identifier id, JsonObject template, Triple<StructureSaver.ItemStackKey, StructureSaver.ItemStackKey, StructureSaver.ItemStackKey> ingredients, HolderLookup.Provider registries) {
            this.id = id;
            FixIngredient(template, ingredients.getLeft(), "1");
            FixIngredient(template, ingredients.getMiddle(), "2");
            FixIngredient(template, ingredients.getRight(), "3");
            // tag ingredients (#c:dyes/blue) need the registries
            this.recipe = ShapedRecipe.MAP_CODEC.codec().parse(registries.createSerializationContext(JsonOps.INSTANCE), template)
                    .getOrThrow(JsonParseException::new);
            this.templateIngredientIndexes = templateIngredientIndexes(GsonHelper.getAsJsonArray(template, "pattern").asList().stream().map(JsonElement::getAsString).toList());
        }

        /**
         * Positions of 1, 2 and 3 once the pattern is shrunk to its non blank rows and columns, like ShapedRecipePattern does.
         */
        private static List<Integer> templateIngredientIndexes(List<String> pattern) {
            List<Integer> rows = IntStream.range(0, pattern.size()).filter(y -> !pattern.get(y).isBlank()).boxed().toList();
            List<Integer> columns = IntStream.range(0, pattern.get(0).length()).filter(x -> pattern.stream().anyMatch(row -> row.charAt(x) != ' ')).boxed().toList();
            if (rows.isEmpty()) return List.of();
            int top = rows.get(0), left = columns.get(0), width = columns.get(columns.size() - 1) - left + 1;
            List<Integer> indexes = new ArrayList<>();
            for (int y = top; y <= rows.get(rows.size() - 1); y++) {
                for (int x = left; x < left + width; x++) {
                    if ("123".indexOf(pattern.get(y).charAt(x)) >= 0) indexes.add(x - left + (y - top) * width);
                }
            }
            return indexes;
        }

        private void FixIngredient(JsonObject template, StructureSaver.ItemStackKey ingredientKey, String key) {
            if (ingredientKey != null) {
                // Add the key for the item, an ingredient being an item id
                template.getAsJsonObject("key").addProperty(key, BuiltInRegistries.ITEM.getKey(ingredientKey.itemStack.getItem()).toString());
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

        public PrefabsBlueprintCapsuleRecipe(Identifier id, ShapedRecipe serializedRecipe, List<Integer> templateIngredientIndexes) {
            this.id = id;
            this.recipe = serializedRecipe;
            this.templateIngredientIndexes = templateIngredientIndexes;
        }

        public Identifier id() {
            return id;
        }

        /**
         * The blueprint of this prefab, as the recipe gives it.
         */
        public ItemStack getResultItem() {
            return recipe.assemble(CraftingInput.EMPTY);
        }

        /**
         * Only blueprint material is consumed. Materials used inside blueprint are given back.
         */
        @Override
        public NonNullList<ItemStack> getRemainingItems(CraftingInput inv) {
            NonNullList<ItemStack> nonnulllist = NonNullList.withSize(inv.size(), ItemStack.EMPTY);
            int[] patternIndexes = patternIndexes(inv);

            for (int i = 0; i < nonnulllist.size(); ++i) {
                ItemStack itemstack = inv.getItem(i);
                nonnulllist.set(i, Services.PLATFORM.getCraftingRemainingItem(itemstack));
                if (itemstack.getItem() instanceof CapsuleItem) {
                    nonnulllist.set(i, ClearCapsuleRecipe.givenBack(itemstack.copy()));
                } else if (patternIndexes != null && templateIngredientIndexes.contains(patternIndexes[i])) {
                    ItemStack refund = itemstack.copy();
                    refund.setCount(1);
                    nonnulllist.set(i, refund);
                }
            }

            return nonnulllist;
        }

        /**
         * Not registered: the prefab recipes reach the client inside the aggregator recipe.
         */
        @Override
        public RecipeSerializer<ShapedRecipe> getSerializer() {
            return ShapedRecipe.SERIALIZER;
        }

        public boolean matches(CraftingInput inv) {
            return patternIndexes(inv) != null;
        }

        /**
         * The recipe pattern index under each slot of inv (-1 outside the pattern) where the recipe matches, or null.
         */
        @Nullable
        private int[] patternIndexes(CraftingInput inv) {
            for (int i = 0; i <= inv.width() - recipe.getWidth(); ++i) {
                for (int j = 0; j <= inv.height() - recipe.getHeight(); ++j) {
                    for (boolean mirrored : new boolean[]{true, false}) {
                        if (this.checkMatch(inv, i, j, mirrored)) {
                            int[] indexes = new int[inv.size()];
                            for (int slot = 0; slot < indexes.length; slot++) {
                                int k = slot % inv.width() - i;
                                int l = slot / inv.width() - j;
                                boolean inside = k >= 0 && l >= 0 && k < recipe.getWidth() && l < recipe.getHeight();
                                indexes[slot] = inside ? (mirrored ? recipe.getWidth() - k - 1 : k) + l * recipe.getWidth() : -1;
                            }
                            return indexes;
                        }
                    }
                }
            }
            return null;
        }

        @Override
        public boolean matches(CraftingInput inv, Level worldIn) {
            return matches(inv);
        }

        /**
         * Checks if the region of a crafting inventory is match for the recipe.
         */
        private boolean checkMatch(CraftingInput craftingInventory, int p_77573_2_, int p_77573_3_, boolean p_77573_4_) {
            for (int i = 0; i < craftingInventory.width(); ++i) {
                for (int j = 0; j < craftingInventory.height(); ++j) {
                    int k = i - p_77573_2_;
                    int l = j - p_77573_3_;
                    Optional<Ingredient> ingredient = Optional.empty();
                    if (k >= 0 && l >= 0 && k < recipe.getWidth() && l < recipe.getHeight()) {
                        if (p_77573_4_) {
                            ingredient = recipe.getIngredients().get(recipe.getWidth() - k - 1 + l * this.recipe.getWidth());
                        } else {
                            ingredient = recipe.getIngredients().get(k + l * recipe.getWidth());
                        }
                    }

                    if (!Ingredient.testOptionalIngredient(ingredient, craftingInventory.getItem(i + j * craftingInventory.width()))) {
                        return false;
                    }
                }
            }

            return true;
        }

        @Override
        public ItemStack assemble(CraftingInput invC) {
            return recipe.assemble(invC);
        }

        @Override
        public boolean showNotification() {
            return recipe.showNotification();
        }

        @Override
        public String group() {
            return recipe.group();
        }

        @Override
        public PlacementInfo placementInfo() {
            return recipe.placementInfo();
        }

        @Override
        public CraftingBookCategory category() {
            return CraftingBookCategory.MISC;
        }
    }
}
