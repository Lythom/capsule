package capsule.enchantments;

import capsule.CapsuleMod;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;

public class CapsuleEnchantments {
    /**
     * Replaced by Loyalty, kept registered for the capsules enchanted with it.
     */
    public static final ResourceKey<Enchantment> RECALL = ResourceKey.create(
            Registries.ENCHANTMENT,
            ResourceLocation.fromNamespaceAndPath(CapsuleMod.MODID, "recall")
    );

    public static final TagKey<Item> RECALLABLE = TagKey.create(
            Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(CapsuleMod.MODID, "enchantable/recall")
    );

    /**
     * Lets the enchanting table and the anvil put Loyalty on capsules, without the other trident enchantments.
     */
    public static boolean acceptsLoyalty(ItemStack stack, Holder<Enchantment> enchantment) {
        return enchantment.is(Enchantments.LOYALTY) && stack.is(RECALLABLE);
    }

    public static boolean comesBack(ItemStack stack) {
        return stack.is(RECALLABLE) && stack.getEnchantments().keySet().stream()
                .anyMatch(enchantment -> enchantment.is(Enchantments.LOYALTY) || enchantment.is(RECALL));
    }
}
