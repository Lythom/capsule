package capsule.plugins.emi;

import capsule.CapsuleMod;
import capsule.items.CapsuleItems;
import capsule.plugins.RecipeViewerContent;
import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiCraftingRecipe;
import dev.emi.emi.api.recipe.EmiInfoRecipe;
import dev.emi.emi.api.stack.Comparison;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.ShapedRecipe;

import java.util.ArrayList;
import java.util.List;

/**
 * Loaded by EMI through the emi entrypoint on Fabric, and through its annotation on NeoForge.
 */
@EmiEntrypoint
public class CapsuleEmiPlugin implements EmiPlugin {

    @Override
    public void register(EmiRegistry registry) {
        registry.setDefaultComparison(CapsuleItems.CAPSULE.get(), Comparison.compareData(stack -> RecipeViewerContent.subtype(stack.getItemStack())));
        if (!RecipeViewerContent.isComplete()) return;
        RecipeViewerContent.craftingRecipes().forEach(recipe -> registry.addRecipe(crafting(recipe)));
        List<RecipeViewerContent.Information> pages = RecipeViewerContent.informationPages();
        for (int i = 0; i < pages.size(); i++) {
            RecipeViewerContent.Information page = pages.get(i);
            registry.addRecipe(new EmiInfoRecipe(page.stacks().stream().<EmiIngredient>map(EmiStack::of).toList(), List.of(page.text()),
                    ResourceLocation.fromNamespaceAndPath(CapsuleMod.MODID, "/info/" + i)));
        }
    }

    private static EmiCraftingRecipe crafting(RecipeHolder<CraftingRecipe> holder) {
        CraftingRecipe recipe = holder.value();
        EmiStack result = EmiStack.of(recipe.getResultItem(Minecraft.getInstance().level.registryAccess()));
        if (!(recipe instanceof ShapedRecipe shaped)) {
            return new EmiCraftingRecipe(recipe.getIngredients().stream().map(EmiIngredient::of).toList(), result, holder.id(), true);
        }
        // a shaped recipe is laid out on the 3x3 grid
        List<EmiIngredient> grid = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            int x = i % 3, y = i / 3;
            grid.add(EmiIngredient.of(x < shaped.getWidth() && y < shaped.getHeight()
                    ? shaped.getIngredients().get(x + y * shaped.getWidth()) : Ingredient.EMPTY));
        }
        return new EmiCraftingRecipe(grid, result, holder.id(), false);
    }
}
