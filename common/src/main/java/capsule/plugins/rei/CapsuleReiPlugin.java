package capsule.plugins.rei;

import capsule.items.CapsuleItems;
import capsule.plugins.RecipeViewerContent;
import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
import me.shedaniel.rei.api.client.registry.display.DisplayRegistry;
import me.shedaniel.rei.api.common.entry.comparison.ItemComparatorRegistry;
import me.shedaniel.rei.api.common.util.EntryIngredients;
import me.shedaniel.rei.plugin.common.displays.DefaultInformationDisplay;
import me.shedaniel.rei.plugin.common.displays.crafting.DefaultCraftingDisplay;

/**
 * Loaded by REI through the rei_client entrypoint on Fabric, and through a subclass annotated for REI on NeoForge.
 */
public class CapsuleReiPlugin implements REIClientPlugin {

    @Override
    public void registerItemComparators(ItemComparatorRegistry registry) {
        registry.register((context, stack) -> RecipeViewerContent.subtype(stack).hashCode(), CapsuleItems.CAPSULE.get());
    }

    @Override
    public void registerDisplays(DisplayRegistry registry) {
        if (!RecipeViewerContent.isComplete()) return;
        RecipeViewerContent.craftingRecipes().forEach(recipe -> registry.add(DefaultCraftingDisplay.of(recipe)));
        for (RecipeViewerContent.Information page : RecipeViewerContent.informationPages()) {
            registry.add(DefaultInformationDisplay.createFromEntries(EntryIngredients.ofItemStacks(page.stacks()),
                    page.stacks().getFirst().getHoverName()).line(page.text()));
        }
    }
}
