package capsule.plugins.jei;

import capsule.CapsuleMod;
import capsule.items.CapsuleItems;
import capsule.plugins.RecipeViewerContent;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.ISubtypeRegistration;
import net.minecraft.resources.Identifier;

import javax.annotation.Nonnull;

@JeiPlugin
public class CapsulePlugin implements IModPlugin {

    @Override
    public void registerItemSubtypes(ISubtypeRegistration subtypeRegistry) {
        subtypeRegistry.registerSubtypeInterpreter(CapsuleItems.CAPSULE.get(), (stack, context) -> RecipeViewerContent.subtype(stack));
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
    public Identifier getPluginUid() {
        return Identifier.fromNamespaceAndPath(CapsuleMod.MODID, "main");
    }
}
