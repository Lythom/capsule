package capsule.enchantments;

import capsule.CapsuleMod;
import capsule.Config;
import capsule.helpers.Spacial;
import capsule.items.CapsuleItem;
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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Objects;

@Mod.EventBusSubscriber(modid = CapsuleMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class RecallEnchant extends Enchantment {
    protected static final Logger LOGGER = LogManager.getLogger(RecallEnchant.class);

    // rarity and category now have to be lazily-inited, since config is not valid at time of enchantment registration
    // querying config then will lead to an IllegalStateException
    private Rarity actualRarity;  // from config
    private boolean categoryInited = false;
    private EnchantmentCategory actualCategory;  // from config

    protected RecallEnchant() {
        // note: rarity & category not important here; we'll lazy-get the actual values from config later
        super(Rarity.COMMON, EnchantmentCategory.WEAPON, EquipmentSlot.values());
    }

    @Override
    public Rarity getRarity() {
        if (actualRarity == null) {
            try {
                actualRarity = Rarity.valueOf(Config.enchantRarity.get());
            } catch (IllegalArgumentException e) {
                LOGGER.warn("Couldn't find the rarity " + Config.enchantRarity.get() + ". Using RARE instead.");
                actualRarity = Rarity.RARE;
            }
        }
        return actualRarity;
    }

    private EnchantmentCategory getActualCategory() {
        if (!categoryInited) {
            try {
                actualCategory = EnchantmentCategory.valueOf(Config.recallEnchantType.get());
            } catch (IllegalArgumentException ignored) {
            }
            categoryInited = true;
        }
        return actualCategory;
    }

    @Override
    public boolean canEnchant(ItemStack stack) {
        return canApplyAtEnchantingTable(stack);
    }

    @Override
    public boolean canApplyAtEnchantingTable(ItemStack stack) {
        return (stack.getItem() instanceof CapsuleItem && !CapsuleItem.isBlueprint(stack) && !CapsuleItem.isOneUse(stack))
                || (this.getActualCategory() != null && this.getActualCategory().canEnchant(stack.getItem()));
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
    public static void onWorldTickEvent(TickEvent.LevelTickEvent wte) {
        if (wte.side == LogicalSide.CLIENT || wte.phase != TickEvent.Phase.END)
            return;

        ServerLevel world = (ServerLevel) wte.level;
        List<? extends ItemEntity> recallEntities = world.getEntities(EntityType.ITEM, CapsuleEnchantments.hasRecallEnchant);
        List<ItemEntity> recallItemEntities = recallEntities.stream()
                .filter(Objects::nonNull)
                .map(entity -> (ItemEntity) entity)
                .toList();

        for (ItemEntity entity : recallItemEntities) {
            if (entity.thrower != null && (entity.horizontalCollision || entity.verticalCollision || Spacial.ItemEntityShouldAndCollideLiquid(entity))) {
                // give the item a last tick
                if (!entity.isInLava()) {
                    entity.tick();
                }
                // then recall to inventory
                if (entity.isAlive()) {
                    pickupItemBack(entity, world.getPlayerByUUID(entity.thrower));
                }
            }
        }
    }

}
