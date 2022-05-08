package capsule.recipes;

import capsule.CapsuleMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleRecipeSerializer;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.event.RegistryEvent;

public class CapsuleRecipes {

    public static final BlueprintCapsuleRecipe.Serializer BLUEPRINT_CAPSULE_SERIALIZER = register("blueprint_capsule", new BlueprintCapsuleRecipe.Serializer());
    public static final SimpleRecipeSerializer<BlueprintChangeRecipe> BLUEPRINT_CHANGE_SERIALIZER = register("blueprint_change", new SimpleRecipeSerializer<>(BlueprintChangeRecipe::new));
    public static final SimpleRecipeSerializer<ClearCapsuleRecipe> CLEAR_CAPSULE_SERIALIZER = register("clear_capsule", new SimpleRecipeSerializer<>(ClearCapsuleRecipe::new));
    public static final SimpleRecipeSerializer<DyeCapsuleRecipe> DYE_CAPSULE_SERIALIZER = register("dye_capsule", new SimpleRecipeSerializer<>(DyeCapsuleRecipe::new));
    public static final RecoveryCapsuleRecipe.Serializer RECOVERY_CAPSULE_SERIALIZER = register("recovery_capsule", new RecoveryCapsuleRecipe.Serializer());
    public static final UpgradeCapsuleRecipe.Serializer UPGRADE_CAPSULE_SERIALIZER = register("upgrade_capsule", new UpgradeCapsuleRecipe.Serializer());
    public static final PrefabsBlueprintAggregatorRecipe.Serializer PREFABS_AGGREGATOR_SERIALIZER = register("aggregate_all_prefabs", new PrefabsBlueprintAggregatorRecipe.Serializer());

    private static <T extends RecipeSerializer<? extends Recipe<?>>> T register(final String name, final T t) {
        t.setRegistryName(new ResourceLocation(CapsuleMod.MODID, name));
        return t;
    }

    public static void registerRecipeSerializers(RegistryEvent.Register<RecipeSerializer<?>> event) {
        CraftingHelper.register(new ResourceLocation("capsule", "ingredient"), CapsuleIngredient.Serializer.INSTANCE);
        event.getRegistry().register(BLUEPRINT_CAPSULE_SERIALIZER);
        event.getRegistry().register(BLUEPRINT_CHANGE_SERIALIZER);
        event.getRegistry().register(CLEAR_CAPSULE_SERIALIZER);
        event.getRegistry().register(DYE_CAPSULE_SERIALIZER);
        event.getRegistry().register(RECOVERY_CAPSULE_SERIALIZER);
        event.getRegistry().register(UPGRADE_CAPSULE_SERIALIZER);
        event.getRegistry().register(PREFABS_AGGREGATOR_SERIALIZER);
    }
}
