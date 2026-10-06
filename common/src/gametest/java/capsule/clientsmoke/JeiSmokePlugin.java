package capsule.clientsmoke;

import capsule.CapsuleMod;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.recipe.IFocus;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Dev only JEI plugin giving the client smoke test access to the JEI runtime.
 */
@JeiPlugin
public class JeiSmokePlugin implements IModPlugin, RecipeViewerProbe {
    private static IJeiRuntime runtime;

    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(CapsuleMod.MODID, "clientsmoke");
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        runtime = jeiRuntime;
    }

    @Override
    public String name() {
        return "JEI";
    }

    @Override
    public boolean ready() {
        return runtime != null;
    }

    @Override
    public long craftingRecipes(ItemStack output) {
        return runtime.getRecipeManager().createRecipeLookup(RecipeTypes.CRAFTING).limitFocus(List.of(focus(output))).get().count();
    }

    @Override
    public long capsuleInformationPages() {
        return runtime.getRecipeManager().createRecipeLookup(RecipeTypes.INFORMATION).get()
                .filter(page -> page.getIngredients().stream()
                        .anyMatch(i -> i.getItemStack().map(RecipeViewerProbe::isCapsule).orElse(false)))
                .count();
    }

    @Override
    public void showRecipes(ItemStack output) {
        runtime.getRecipesGui().show(focus(output));
    }

    private static IFocus<ItemStack> focus(ItemStack stack) {
        return runtime.getJeiHelpers().getFocusFactory().createFocus(RecipeIngredientRole.OUTPUT, VanillaTypes.ITEM_STACK, stack);
    }
}
