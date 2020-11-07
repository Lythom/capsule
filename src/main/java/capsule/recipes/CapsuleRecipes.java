package capsule.recipes;

import capsule.CapsuleMod;
import capsule.blocks.TileEntityCapture;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraft.item.crafting.SpecialRecipeSerializer;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;

public class CapsuleRecipes {

    public static final BlueprintCapsuleRecipe.Serializer BLUEPRINT_CAPSULE_SERIALIZER = register("blueprint_capsule", new BlueprintCapsuleRecipe.Serializer());
    public static final SpecialRecipeSerializer<BlueprintChangeRecipe> BLUEPRINT_CHANGE_SERIALIZER = register("blueprint_change", new SpecialRecipeSerializer<>(BlueprintChangeRecipe::new));
    public static final SpecialRecipeSerializer<ClearCapsuleRecipe> CLEAR_CAPSULE_SERIALIZER = register("clear_capsule", new SpecialRecipeSerializer<>(ClearCapsuleRecipe::new));
    public static final SpecialRecipeSerializer<DyeCapsuleRecipe> DYE_CAPSULE_SERIALIZER = register("dye_capsule", new SpecialRecipeSerializer<>(DyeCapsuleRecipe::new));
    public static final SpecialRecipeSerializer<PrefabsBlueprintAggregatorRecipe> PREFABS_AGGREGATOR_SERIALIZER = register("aggregate_all_prefabs", new SpecialRecipeSerializer<>(PrefabsBlueprintAggregatorRecipe::new));
    public static final RecoveryCapsuleRecipe.Serializer RECOVERY_CAPSULE_SERIALIZER = register("recovery_capsule", new RecoveryCapsuleRecipe.Serializer());
    public static final UpgradeCapsuleRecipe.Serializer UPGRADE_CAPSULE_SERIALIZER = register("upgrade_capsule", new UpgradeCapsuleRecipe.Serializer());

    private static <T extends IRecipeSerializer<? extends IRecipe<?>>> T register(final String name, final T t) {
        t.setRegistryName(new ResourceLocation(CapsuleMod.MODID, name));
        return t;
    }

    public static void registerRecipeSerializers(RegistryEvent.Register<IRecipeSerializer<?>> event) {
        event.getRegistry().register(BLUEPRINT_CAPSULE_SERIALIZER);
        event.getRegistry().register(BLUEPRINT_CHANGE_SERIALIZER);
        event.getRegistry().register(CLEAR_CAPSULE_SERIALIZER);
        event.getRegistry().register(DYE_CAPSULE_SERIALIZER);
        event.getRegistry().register(PREFABS_AGGREGATOR_SERIALIZER);
        event.getRegistry().register(RECOVERY_CAPSULE_SERIALIZER);
        event.getRegistry().register(UPGRADE_CAPSULE_SERIALIZER);
    }
}
