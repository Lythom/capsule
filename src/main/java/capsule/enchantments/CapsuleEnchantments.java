package capsule.enchantments;

import capsule.CapsuleMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

import java.util.function.Predicate;

public class CapsuleEnchantments {
    public static final ResourceKey<Enchantment> RECALL = ResourceKey.create(
            Registries.ENCHANTMENT,
            ResourceLocation.fromNamespaceAndPath(CapsuleMod.MODID, "recall")
    );

    public static final Predicate<Entity> hasRecallEnchant = (Entity entityIn) -> {
        if (entityIn instanceof ItemEntity itemEntity) {
            ItemStack stack = itemEntity.getItem();
            return EnchantmentHelper.getItemEnchantmentLevel(
                    entityIn.level().holderOrThrow(RECALL),
                    stack
            ) > 0;
        }
        return false;
    };
}
