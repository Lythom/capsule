package capsule.plugins.rei;

import capsule.items.CapsuleItems;
import capsule.plugins.RecipeViewerContent;
import me.shedaniel.rei.api.common.entry.comparison.ItemComparatorRegistry;
import me.shedaniel.rei.api.common.plugins.REICommonPlugin;

/**
 * Tells REI the capsule stacks to list separately. Loaded by REI through the rei_common entrypoint on Fabric, and
 * through a subclass annotated for REI on NeoForge.
 */
public class CapsuleReiCommonPlugin implements REICommonPlugin {

    @Override
    public void registerItemComparators(ItemComparatorRegistry registry) {
        registry.register((context, stack) -> RecipeViewerContent.subtype(stack).hashCode(), CapsuleItems.CAPSULE.get());
    }
}
