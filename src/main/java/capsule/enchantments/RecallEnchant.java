package capsule.enchantments;

import capsule.CapsuleMod;
import capsule.helpers.Spacial;
import capsule.items.CapsuleItem;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;

import java.util.List;
import java.util.stream.Collectors;

@Mod.EventBusSubscriber(modid = CapsuleMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class RecallEnchant extends Enchantment {

    protected RecallEnchant(ResourceLocation enchName, Rarity rarity, EnchantmentType enchType) {
        super(rarity, enchType, EquipmentSlotType.values());
        this.setRegistryName(enchName);
    }

    @Override
    public boolean canApply(ItemStack stack) {
        return canApplyAtEnchantingTable(stack);
    }

    @Override
    public boolean canApplyAtEnchantingTable(ItemStack stack) {
        return stack.getItem() instanceof CapsuleItem || this.type != null && super.canApplyAtEnchantingTable(stack);
    }

    @Override
    public int getMinEnchantability(int enchantmentLevel) {
        return 1;
    }

    @Override
    public int getMaxEnchantability(int enchantmentLevel) {
        return this.getMinEnchantability(enchantmentLevel) + 40;
    }

    @Override
    public int getMaxLevel() {
        return 1;
    }

    public void pickupItemBack(ItemEntity entity, PlayerEntity player) {

        if (player != null) {
            entity.setNoPickupDelay();
            entity.onCollideWithPlayer(player);
        }

    }

    @SubscribeEvent
    public void onWorldTickEvent(TickEvent.WorldTickEvent wte) {
        if (wte.side == LogicalSide.CLIENT || wte.phase != TickEvent.Phase.END)
            return;

        ServerWorld world = (ServerWorld) wte.world;
        List<Entity> recallEntities = world.getEntities(EntityType.ITEM, Enchantments.hasRecallEnchant);
        List<ItemEntity> recallItemEntities = recallEntities.stream()
                .filter(entity -> entity instanceof ItemEntity)
                .map(entity -> (ItemEntity)entity)
                .collect(Collectors.toList());

        for (ItemEntity entity : recallItemEntities) {
            if (entity.getThrowerId() != null && (entity.collided || Spacial.ItemEntityShouldAndCollideLiquid(entity))) {
                // give the item a last tick
                if (!entity.isInLava()) {
                    entity.tick();
                }
                // then recall to inventory
                if (entity.isAlive()) {
                    this.pickupItemBack(entity, world.getPlayerByUuid(entity.getThrowerId()));
                }
            }
        }
    }

}
