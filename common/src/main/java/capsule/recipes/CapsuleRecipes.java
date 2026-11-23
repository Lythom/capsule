package capsule.recipes;

import capsule.platform.Services;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;

import java.util.List;
import java.util.function.Supplier;

public class CapsuleRecipes {

    public static final Supplier<RecipeSerializer<BlueprintCapsuleRecipe>> BLUEPRINT_CAPSULE_SERIALIZER = Services.PLATFORM.register(BuiltInRegistries.RECIPE_SERIALIZER, "blueprint_capsule", () -> BlueprintCapsuleRecipe.SERIALIZER);
    public static final Supplier<RecipeSerializer<BlueprintChangeRecipe>> BLUEPRINT_CHANGE_SERIALIZER = Services.PLATFORM.register(BuiltInRegistries.RECIPE_SERIALIZER, "blueprint_change", () -> BlueprintChangeRecipe.SERIALIZER);
    public static final Supplier<RecipeSerializer<ClearCapsuleRecipe>> CLEAR_CAPSULE_SERIALIZER = Services.PLATFORM.register(BuiltInRegistries.RECIPE_SERIALIZER, "clear_capsule", () -> ClearCapsuleRecipe.SERIALIZER);
    public static final Supplier<RecipeSerializer<DyeCapsuleRecipe>> DYE_CAPSULE_SERIALIZER = Services.PLATFORM.register(BuiltInRegistries.RECIPE_SERIALIZER, "dye_capsule", () -> DyeCapsuleRecipe.SERIALIZER);
    public static final Supplier<RecipeSerializer<RecoveryCapsuleRecipe>> RECOVERY_CAPSULE_SERIALIZER = Services.PLATFORM.register(BuiltInRegistries.RECIPE_SERIALIZER, "recovery_capsule", () -> RecoveryCapsuleRecipe.SERIALIZER);
    public static final Supplier<RecipeSerializer<UpgradeCapsuleRecipe>> UPGRADE_CAPSULE_SERIALIZER = Services.PLATFORM.register(BuiltInRegistries.RECIPE_SERIALIZER, "upgrade_capsule", () -> UpgradeCapsuleRecipe.SERIALIZER);
    public static final Supplier<RecipeSerializer<PrefabsBlueprintAggregatorRecipe>> PREFABS_AGGREGATOR_SERIALIZER = Services.PLATFORM.register(BuiltInRegistries.RECIPE_SERIALIZER, "aggregate_all_prefabs", () -> PrefabsBlueprintAggregatorRecipe.SERIALIZER);

    /**
     * The serializers of the recipes the client needs (creative tab, recipe viewers, prefab recipes): Capsule's, and
     * the vanilla shaped recipe of the capsule tiers. Since Minecraft 1.21.2 the server sends recipes to the client
     * only when a loader is asked to (NeoForge sends every crafting recipe, Fabric the recipes of these serializers).
     */
    public static List<RecipeSerializer<?>> clientSyncedSerializers() {
        return List.of(BLUEPRINT_CAPSULE_SERIALIZER.get(), BLUEPRINT_CHANGE_SERIALIZER.get(), CLEAR_CAPSULE_SERIALIZER.get(),
                DYE_CAPSULE_SERIALIZER.get(), RECOVERY_CAPSULE_SERIALIZER.get(), UPGRADE_CAPSULE_SERIALIZER.get(),
                PREFABS_AGGREGATOR_SERIALIZER.get(), ShapedRecipe.SERIALIZER);
    }

    public static void init() {
    }
}
