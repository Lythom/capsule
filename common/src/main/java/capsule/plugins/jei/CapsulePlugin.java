package capsule.plugins.jei;

import capsule.CapsuleMod;
import capsule.items.CapsuleItems;
import capsule.plugins.RecipeViewerContent;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.ingredients.subtypes.ISubtypeInterpreter;
import mezz.jei.api.ingredients.subtypes.UidContext;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.ISubtypeRegistration;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;

@JeiPlugin
public class CapsulePlugin implements IModPlugin {

    @Override
    public void registerItemSubtypes(ISubtypeRegistration subtypeRegistry) {
        subtypeRegistry.registerSubtypeInterpreter(CapsuleItems.CAPSULE.get(), new ISubtypeInterpreter<>() {
            @Override
            public Object getSubtypeData(ItemStack stack, UidContext context) {
                return RecipeViewerContent.subtype(stack);
            }

            @Override
            public String getLegacyStringSubtypeInfo(ItemStack stack, UidContext context) {
                return RecipeViewerContent.subtype(stack);
            }
        });
    }

    @Override
    public void registerRecipes(@Nonnull IRecipeRegistration registry) {
        if (!RecipeViewerContent.isComplete()) return;
        registry.addRecipes(RecipeTypes.CRAFTING, RecipeViewerContent.craftingRecipes());
        for (RecipeViewerContent.Information page : RecipeViewerContent.informationPages()) {
            registry.addIngredientInfo(page.stacks(), VanillaTypes.ITEM_STACK, page.text());
        }
    }

    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(CapsuleMod.MODID, "main");
    }
}
