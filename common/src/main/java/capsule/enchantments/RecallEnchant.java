package capsule.enchantments;

import capsule.helpers.Spacial;
import capsule.items.ThrownCapsules;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class RecallEnchant {
    protected static final Logger LOGGER = LogManager.getLogger(RecallEnchant.class);

    public static void pickupItemBack(ItemEntity entity, Player player) {
        if (player != null) {
            entity.setNoPickUpDelay();
            entity.playerTouch(player);
        }
    }

    /**
     * A thrown capsule cannot deploy before its third tick (see CapsuleItem.onEntityItemUpdate): recalling it earlier would
     * bring it back undeployed.
     */
    public static boolean shouldRecall(ItemEntity entity) {
        return entity.getOwner() != null
                && entity.tickCount > 2
                && (entity.horizontalCollision || entity.verticalCollision || Spacial.ItemEntityShouldAndCollideLiquid(entity));
    }

    public static void onWorldTickEvent(ServerLevel world) {
        for (ItemEntity entity : ThrownCapsules.in(world)) {
            if (CapsuleEnchantments.comesBack(entity.getItem()) && shouldRecall(entity)) {
                // give the item a last tick
                if (!entity.isInLava()) {
                    entity.tick();
                }
                // then recall to inventory
                if (entity.isAlive()) {
                    pickupItemBack(entity, world.getPlayerByUUID(entity.getOwner().getUUID()));
                }
            }
        }
    }
}
