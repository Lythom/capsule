package capsule.recipes;

import capsule.CapsuleMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleRecipeSerializer;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class CapsuleRecipes {

    private static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, CapsuleMod.MODID);

    public static final RegistryObject<BlueprintCapsuleRecipe.Serializer> BLUEPRINT_CAPSULE_SERIALIZER = RECIPE_SERIALIZERS.register("blueprint_capsule", BlueprintCapsuleRecipe.Serializer::new);
    public static final RegistryObject<SimpleRecipeSerializer<BlueprintChangeRecipe>> BLUEPRINT_CHANGE_SERIALIZER = RECIPE_SERIALIZERS.register("blueprint_change", () -> new SimpleRecipeSerializer<>(BlueprintChangeRecipe::new));
    public static final RegistryObject<SimpleRecipeSerializer<ClearCapsuleRecipe>> CLEAR_CAPSULE_SERIALIZER = RECIPE_SERIALIZERS.register("clear_capsule", () -> new SimpleRecipeSerializer<>(ClearCapsuleRecipe::new));
    public static final RegistryObject<SimpleRecipeSerializer<DyeCapsuleRecipe>> DYE_CAPSULE_SERIALIZER = RECIPE_SERIALIZERS.register("dye_capsule", () -> new SimpleRecipeSerializer<>(DyeCapsuleRecipe::new));
    public static final RegistryObject<RecoveryCapsuleRecipe.Serializer> RECOVERY_CAPSULE_SERIALIZER = RECIPE_SERIALIZERS.register("recovery_capsule", RecoveryCapsuleRecipe.Serializer::new);
    public static final RegistryObject<UpgradeCapsuleRecipe.Serializer> UPGRADE_CAPSULE_SERIALIZER = RECIPE_SERIALIZERS.register("upgrade_capsule", UpgradeCapsuleRecipe.Serializer::new);
    public static final RegistryObject<PrefabsBlueprintAggregatorRecipe.Serializer> PREFABS_AGGREGATOR_SERIALIZER = RECIPE_SERIALIZERS.register("aggregate_all_prefabs", PrefabsBlueprintAggregatorRecipe.Serializer::new);

    public static void registerRecipeSerializers(IEventBus event) {
        RECIPE_SERIALIZERS.register(event);
        CraftingHelper.register(new ResourceLocation("capsule", "ingredient"), CapsuleIngredient.Serializer.INSTANCE);
    }
}
