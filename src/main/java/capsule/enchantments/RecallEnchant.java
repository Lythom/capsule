package capsule.enchantments;

import capsule.helpers.Spacial;
import capsule.items.CapsuleItem;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnumEnchantmentType;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.gameevent.TickEvent.WorldTickEvent;
import net.minecraftforge.fml.relauncher.Side;

import java.util.List;

public class RecallEnchant extends Enchantment {

    protected RecallEnchant(ResourceLocation enchName, Rarity rarity, EnumEnchantmentType enchType) {
        super(rarity, enchType, EntityEquipmentSlot.values());
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

    public void pickupItemBack(EntityItem entity, EntityPlayer player) {

        if (player != null) {
            entity.setNoPickupDelay();
            entity.onCollideWithPlayer(player);
        }

    }

    @SubscribeEvent
    public void onWorldTickEvent(WorldTickEvent wte) {

        if (wte.side == Side.CLIENT || wte.phase != Phase.END)
            return;

        WorldServer world = (WorldServer) wte.world;
        @SuppressWarnings("unchecked")
        List<EntityItem> recallEntities = world.<EntityItem>getEntities(EntityItem.class, Enchantments.hasRecallEnchant::test);
        for (EntityItem entity : recallEntities) {
            if (entity.getThrower() != null && (entity.collided || Spacial.entityItemShouldAndCollideLiquid(entity))) {
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
