package capsule.clientsmoke;

import capsule.CapsuleMod;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.recipe.IFocus;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Dev only JEI plugin giving the client smoke test access to the JEI runtime. JEI loads it on NeoForge only.
 */
@JeiPlugin
public class JeiSmokePlugin implements IModPlugin {
    private static IJeiRuntime runtime;

    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(CapsuleMod.MODID, "clientsmoke");
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        runtime = jeiRuntime;
    }

    static boolean available() {
        return runtime != null;
    }

    static long craftingRecipes(ItemStack output) {
        return count(RecipeTypes.CRAFTING, output);
    }

    /**
     * Information pages of capsule items, which only the capsule plugin registers.
     */
    static long capsuleInformationPages() {
        return runtime.getRecipeManager().createRecipeLookup(RecipeTypes.INFORMATION).get()
                .filter(page -> page.getIngredients().stream().anyMatch(i -> i.getItemStack()
                        .map(stack -> BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace().equals(CapsuleMod.MODID))
                        .orElse(false)))
                .count();
    }

    static void showRecipes(ItemStack output) {
        runtime.getRecipesGui().show(focus(output));
    }

    private static long count(RecipeType<?> type, ItemStack output) {
        return runtime.getRecipeManager().createRecipeLookup(type).limitFocus(List.of(focus(output))).get().count();
    }

    private static IFocus<ItemStack> focus(ItemStack stack) {
        return runtime.getJeiHelpers().getFocusFactory().createFocus(RecipeIngredientRole.OUTPUT, VanillaTypes.ITEM_STACK, stack);
    }
}
