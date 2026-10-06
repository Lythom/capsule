package capsule.clientsmoke;

import capsule.CapsuleMod;
import dev.emi.emi.api.EmiApi;
import dev.emi.emi.api.recipe.VanillaEmiRecipeCategories;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

class EmiProbe implements RecipeViewerProbe {

    @Override
    public String name() {
        return "EMI";
    }

    @Override
    public boolean ready() {
        // EMI loads its plugins in the background
        return EmiApi.getRecipeManager().getRecipe(ResourceLocation.fromNamespaceAndPath(CapsuleMod.MODID, "/info/0")) != null;
    }

    @Override
    public long craftingRecipes(ItemStack output) {
        return EmiApi.getRecipeManager().getRecipesByOutput(EmiStack.of(output)).stream()
                .filter(recipe -> recipe.getCategory() == VanillaEmiRecipeCategories.CRAFTING)
                .count();
    }

    @Override
    public long capsuleInformationPages() {
        return EmiApi.getRecipeManager().getRecipes(VanillaEmiRecipeCategories.INFO).stream()
                .filter(page -> page.getInputs().stream().flatMap(i -> i.getEmiStacks().stream())
                        .anyMatch(stack -> RecipeViewerProbe.isCapsule(stack.getItemStack())))
                .count();
    }

    @Override
    public void showRecipes(ItemStack output) {
        EmiApi.displayRecipes(EmiStack.of(output));
    }
}
