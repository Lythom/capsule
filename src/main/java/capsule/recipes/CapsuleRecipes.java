package capsule.recipes;

import capsule.CapsuleMod;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class CapsuleRecipes {

    private static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister.create(BuiltInRegistries.RECIPE_SERIALIZER, CapsuleMod.MODID);

    public static final Supplier<BlueprintCapsuleRecipe.Serializer> BLUEPRINT_CAPSULE_SERIALIZER = RECIPE_SERIALIZERS.register("blueprint_capsule", BlueprintCapsuleRecipe.Serializer::new);
    public static final Supplier<SimpleCraftingRecipeSerializer<BlueprintChangeRecipe>> BLUEPRINT_CHANGE_SERIALIZER = RECIPE_SERIALIZERS.register("blueprint_change", () -> new SimpleCraftingRecipeSerializer<>(BlueprintChangeRecipe::new));
    public static final Supplier<SimpleCraftingRecipeSerializer<ClearCapsuleRecipe>> CLEAR_CAPSULE_SERIALIZER = RECIPE_SERIALIZERS.register("clear_capsule", () -> new SimpleCraftingRecipeSerializer<>(ClearCapsuleRecipe::new));
    public static final Supplier<SimpleCraftingRecipeSerializer<DyeCapsuleRecipe>> DYE_CAPSULE_SERIALIZER = RECIPE_SERIALIZERS.register("dye_capsule", () -> new SimpleCraftingRecipeSerializer<>(DyeCapsuleRecipe::new));
    public static final Supplier<RecoveryCapsuleRecipe.Serializer> RECOVERY_CAPSULE_SERIALIZER = RECIPE_SERIALIZERS.register("recovery_capsule", RecoveryCapsuleRecipe.Serializer::new);
    public static final Supplier<UpgradeCapsuleRecipe.Serializer> UPGRADE_CAPSULE_SERIALIZER = RECIPE_SERIALIZERS.register("upgrade_capsule", UpgradeCapsuleRecipe.Serializer::new);
    public static final Supplier<PrefabsBlueprintAggregatorRecipe.Serializer> PREFABS_AGGREGATOR_SERIALIZER = RECIPE_SERIALIZERS.register("aggregate_all_prefabs", PrefabsBlueprintAggregatorRecipe.Serializer::new);

    public static void registerRecipeSerializers(IEventBus event) {
        RECIPE_SERIALIZERS.register(event);
//        CraftingHelper.register(ResourceLocation.fromNamespaceAndPath("capsule", "ingredient"), CapsuleIngredient.Serializer.INSTANCE); //TODO: Re-implement capsule ingredient
    }
}
