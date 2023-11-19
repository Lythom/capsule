package capsule.itemGroups;

import capsule.CapsuleMod;
import capsule.blocks.CapsuleBlocks;
import capsule.items.CapsuleItem;
import capsule.items.CapsuleItems;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import org.apache.commons.lang3.tuple.Pair;

import static capsule.items.CapsuleItem.CapsuleState.LINKED;

public class CapsuleCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, CapsuleMod.MODID);

    public static final RegistryObject<CreativeModeTab> CAPSULE_TAB = CREATIVE_MODE_TABS.register("tab", () -> CreativeModeTab.builder()
            .icon(() -> {
                ItemStack stack = new ItemStack(CapsuleItems.CAPSULE.get(), 1);
                CapsuleItem.setState(stack, LINKED);
                return stack;
            })
            .withTabsBefore(CreativeModeTabs.SPAWN_EGGS)
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

    public static void registerTabs(IEventBus modEventBus) {
        CREATIVE_MODE_TABS.register(modEventBus);
    }
}
