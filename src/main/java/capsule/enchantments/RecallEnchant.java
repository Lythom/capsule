package capsule.enchantments;

import capsule.helpers.Spacial;
import capsule.items.CapsuleItem;
import com.google.common.base.Strings;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnumEnchantmentType;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.ServerWorld;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.gameevent.TickEvent.WorldTickEvent;
import net.minecraftforge.fml.relauncher.Side;

import java.util.List;

public class RecallEnchant extends Enchantment {

    protected RecallEnchant(ResourceLocation enchName, Rarity rarity, EnumEnchantmentType enchType) {
        super(rarity, enchType, EquipmentSlotType.values());
        this.setName("recall");
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
    public void onWorldTickEvent(WorldTickEvent wte) {

        if (wte.side == Side.CLIENT || wte.phase != Phase.END)
            return;

        ServerWorld world = (ServerWorld) wte.world;
        @SuppressWarnings("unchecked")
        List<ItemEntity> recallEntities = world.<ItemEntity>getEntities(ItemEntity.class, Enchantments.hasRecallEnchant::test);
        for (ItemEntity entity : recallEntities) {
            if (!Strings.isNullOrEmpty(entity.getThrower()) && (entity.collided || Spacial.ItemEntityShouldAndCollideLiquid(entity))) {
                // give the item a last tick
                if (!entity.isInLava()) {
                    entity.onUpdate();
                }
                // then recall to inventory
                if (!entity.isDead) {
                    this.pickupItemBack(entity, world.getPlayerEntityByName(entity.getThrower()));
                }
            }
        }
    }

}
