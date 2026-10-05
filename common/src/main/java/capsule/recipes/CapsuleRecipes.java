package capsule.recipes;

import capsule.platform.Services;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;

import java.util.function.Supplier;

public class CapsuleRecipes {

    public static final Supplier<BlueprintCapsuleRecipe.Serializer> BLUEPRINT_CAPSULE_SERIALIZER = Services.PLATFORM.register(BuiltInRegistries.RECIPE_SERIALIZER, "blueprint_capsule", BlueprintCapsuleRecipe.Serializer::new);
    public static final Supplier<SimpleCraftingRecipeSerializer<BlueprintChangeRecipe>> BLUEPRINT_CHANGE_SERIALIZER = Services.PLATFORM.register(BuiltInRegistries.RECIPE_SERIALIZER, "blueprint_change", () -> new SimpleCraftingRecipeSerializer<>(BlueprintChangeRecipe::new));
    public static final Supplier<SimpleCraftingRecipeSerializer<ClearCapsuleRecipe>> CLEAR_CAPSULE_SERIALIZER = Services.PLATFORM.register(BuiltInRegistries.RECIPE_SERIALIZER, "clear_capsule", () -> new SimpleCraftingRecipeSerializer<>(ClearCapsuleRecipe::new));
    public static final Supplier<SimpleCraftingRecipeSerializer<DyeCapsuleRecipe>> DYE_CAPSULE_SERIALIZER = Services.PLATFORM.register(BuiltInRegistries.RECIPE_SERIALIZER, "dye_capsule", () -> new SimpleCraftingRecipeSerializer<>(DyeCapsuleRecipe::new));
    public static final Supplier<RecoveryCapsuleRecipe.Serializer> RECOVERY_CAPSULE_SERIALIZER = Services.PLATFORM.register(BuiltInRegistries.RECIPE_SERIALIZER, "recovery_capsule", RecoveryCapsuleRecipe.Serializer::new);
    public static final Supplier<UpgradeCapsuleRecipe.Serializer> UPGRADE_CAPSULE_SERIALIZER = Services.PLATFORM.register(BuiltInRegistries.RECIPE_SERIALIZER, "upgrade_capsule", UpgradeCapsuleRecipe.Serializer::new);
    public static final Supplier<PrefabsBlueprintAggregatorRecipe.Serializer> PREFABS_AGGREGATOR_SERIALIZER = Services.PLATFORM.register(BuiltInRegistries.RECIPE_SERIALIZER, "aggregate_all_prefabs", PrefabsBlueprintAggregatorRecipe.Serializer::new);

    public static void init() {
//        CraftingHelper.register(ResourceLocation.fromNamespaceAndPath("capsule", "ingredient"), CapsuleIngredient.Serializer.INSTANCE); //TODO: Re-implement capsule ingredient
    }
}
