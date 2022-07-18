package capsule.enchantments;

import capsule.CapsuleMod;
import capsule.helpers.Spacial;
import capsule.items.CapsuleItem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Mod.EventBusSubscriber(modid = CapsuleMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class RecallEnchant extends Enchantment {

    protected RecallEnchant(Rarity rarity, EnchantmentCategory enchType) {
        super(rarity, enchType, EquipmentSlot.values());
    }

    @Override
    public boolean canEnchant(ItemStack stack) {
        return canApplyAtEnchantingTable(stack);
    }

    @Override
    public boolean canApplyAtEnchantingTable(ItemStack stack) {
        return (stack.getItem() instanceof CapsuleItem && !CapsuleItem.isBlueprint(stack) && !CapsuleItem.isOneUse(stack))
                || (this.category != null && super.canApplyAtEnchantingTable(stack));
    }

    @Override
    public int getMinCost(int enchantmentLevel) {
        return 1;
    }

    @Override
    public int getMaxCost(int enchantmentLevel) {
        return this.getMinCost(enchantmentLevel) + 40;
    }

    @Override
    public int getMaxLevel() {
        return 1;
    }

    public static void pickupItemBack(ItemEntity entity, Player player) {
        if (player != null) {
            entity.setNoPickUpDelay();
            entity.playerTouch(player);
        }
    }

    @SubscribeEvent
    public static void onWorldTickEvent(TickEvent.WorldTickEvent wte) {
        if (wte.side == LogicalSide.CLIENT || wte.phase != TickEvent.Phase.END)
            return;

        ServerLevel world = (ServerLevel) wte.world;
        List<? extends ItemEntity> recallEntities = world.getEntities(EntityType.ITEM, CapsuleEnchantments.hasRecallEnchant);
        List<ItemEntity> recallItemEntities = recallEntities.stream()
                .filter(Objects::nonNull)
                .map(entity -> (ItemEntity) entity)
                .collect(Collectors.toList());

        for (ItemEntity entity : recallItemEntities) {
            if (entity.getThrower() != null && (entity.horizontalCollision || entity.verticalCollision || Spacial.ItemEntityShouldAndCollideLiquid(entity))) {
                // give the item a last tick
                if (!entity.isInLava()) {
                    entity.tick();
                }
                // then recall to inventory
                if (entity.isAlive()) {
                    pickupItemBack(entity, world.getPlayerByUUID(entity.getThrower()));
                }
            }
        }
    }

}
