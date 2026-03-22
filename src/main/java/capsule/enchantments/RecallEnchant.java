package capsule.enchantments;

import capsule.CapsuleMod;
import capsule.helpers.Spacial;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Objects;

@EventBusSubscriber(modid = CapsuleMod.MODID)
public class RecallEnchant {
    protected static final Logger LOGGER = LogManager.getLogger(RecallEnchant.class);

    public static void pickupItemBack(ItemEntity entity, Player player) {
        if (player != null) {
            entity.setNoPickUpDelay();
            entity.playerTouch(player);
        }
    }

    @SubscribeEvent
    public static void onWorldTickEvent(LevelTickEvent.Post event) {
        if (event.getLevel().isClientSide())
            return;

        ServerLevel world = (ServerLevel) event.getLevel();
        List<? extends ItemEntity> recallEntities = world.getEntities(EntityType.ITEM, CapsuleEnchantments.hasRecallEnchant);
        List<ItemEntity> recallItemEntities = recallEntities.stream()
                .filter(Objects::nonNull)
                .map(entity -> (ItemEntity) entity)
                .toList();

        for (ItemEntity entity : recallItemEntities) {
            Entity owner = entity.getOwner();
            if (owner != null && (entity.horizontalCollision || entity.verticalCollision || Spacial.ItemEntityShouldAndCollideLiquid(entity))) {
                // give the item a last tick
                if (!entity.isInLava()) {
                    entity.tick();
                }
                // then recall to inventory
                if (entity.isAlive()) {
                    pickupItemBack(entity, world.getPlayerByUUID(owner.getUUID()));
                }
            }
        }
    }
}
