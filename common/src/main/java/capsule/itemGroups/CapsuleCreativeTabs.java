package capsule.itemGroups;

import capsule.blocks.CapsuleBlocks;
import capsule.items.CapsuleItem;
import capsule.items.CapsuleItems;
import capsule.platform.Services;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import org.apache.commons.lang3.tuple.Pair;

import java.util.function.Supplier;

import static capsule.items.CapsuleItem.CapsuleState.LINKED;

public class CapsuleCreativeTabs {
    public static final Supplier<CreativeModeTab> CAPSULE_TAB = Services.PLATFORM.register(BuiltInRegistries.CREATIVE_MODE_TAB, "tab", () -> Services.PLATFORM.creativeTabBuilder()
            .icon(() -> {
                ItemStack stack = new ItemStack(CapsuleItems.CAPSULE.get(), 1);
                CapsuleItem.setState(stack, LINKED);
                return stack;
            })
            .title(Component.translatable("itemGroup.capsule"))
            .displayItems((parameters, output) -> {
                output.acceptAll(CapsuleItems.capsuleList.keySet());
                output.acceptAll(CapsuleItems.opCapsuleList.keySet());
                if (CapsuleItems.unlabelledCapsule != null) output.accept(CapsuleItems.unlabelledCapsule.getKey());
                if (CapsuleItems.deployedCapsule != null) output.accept(CapsuleItems.deployedCapsule.getKey());
                if (CapsuleItems.recoveryCapsule != null) output.accept(CapsuleItems.recoveryCapsule.getKey());
                if (CapsuleItems.blueprintChangedCapsule != null)
                    output.accept(CapsuleItems.blueprintChangedCapsule.getKey());
                for (Pair<ItemStack, CraftingRecipe> blueprintCapsule : CapsuleItems.blueprintCapsules) {
                    output.accept(blueprintCapsule.getKey());
                }
                for (Pair<ItemStack, CraftingRecipe> blueprintCapsule : CapsuleItems.blueprintPrefabs) {
                    output.accept(blueprintCapsule.getKey());
                }

                output.accept(CapsuleBlocks.CAPSULE_MARKER_ITEM.get());
            }).build());

    public static void init() {
    }
}
