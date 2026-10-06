package capsule.clientsmoke;

import me.shedaniel.rei.api.client.registry.display.DisplayRegistry;
import me.shedaniel.rei.api.client.view.ViewSearchBuilder;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.entry.EntryStack;
import me.shedaniel.rei.api.common.entry.type.VanillaEntryTypes;
import me.shedaniel.rei.api.common.plugins.PluginManager;
import me.shedaniel.rei.api.common.util.EntryStacks;
import me.shedaniel.rei.plugin.common.BuiltinPlugin;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.function.Predicate;

class ReiProbe implements RecipeViewerProbe {

    @Override
    public String name() {
        return "REI";
    }

    @Override
    public boolean ready() {
        return !PluginManager.areAnyReloading() && capsuleInformationPages() > 0;
    }

    @Override
    public long craftingRecipes(ItemStack output) {
        EntryStack<ItemStack> target = EntryStack.of(VanillaEntryTypes.ITEM, output);
        return DisplayRegistry.getInstance().get(BuiltinPlugin.CRAFTING).stream()
                .filter(display -> anyEntry(display.getOutputEntries(), entry -> EntryStacks.equalsExact(entry, target)))
                .count();
    }

    @Override
    public long capsuleInformationPages() {
        return DisplayRegistry.getInstance().get(BuiltinPlugin.INFO).stream()
                .filter(page -> anyEntry(page.getInputEntries(), entry -> entry.getValue() instanceof ItemStack stack && RecipeViewerProbe.isCapsule(stack)))
                .count();
    }

    @Override
    public void showRecipes(ItemStack output) {
        ViewSearchBuilder.builder().addRecipesFor(EntryStack.of(VanillaEntryTypes.ITEM, output)).open();
    }

    private static boolean anyEntry(List<EntryIngredient> ingredients, Predicate<EntryStack<?>> predicate) {
        return ingredients.stream().flatMap(List::stream).anyMatch(predicate);
    }
}
