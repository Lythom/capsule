package capsule.plugins.securitycraft;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.fml.ModList;
import org.apache.commons.lang3.ClassUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * SecurityCraft blocks can only be taken by their owner. SecurityCraft is not a compile dependency: its
 * IOwnable.isOwnedBy API is called by reflection (isOwnedBy(Entity) since v1.9.9, isOwnedBy(Player) before),
 * and a SecurityCraft block is protected if the call fails.
 */
public class SecurityCraftOwnerCheck {
    private static final Logger LOGGER = LogManager.getLogger(SecurityCraftOwnerCheck.class);
    private static final String OWNABLE = "net.geforcemods.securitycraft.api.IOwnable";

    public static boolean canTakeBlock(ServerLevel worldserver, BlockPos blockPos, Player player) {
        if (!ModList.get().isLoaded("securitycraft")) return true;
        BlockEntity blockEntity = worldserver.getBlockEntity(blockPos);
        if (blockEntity == null) return true;
        Class<?> ownable = ClassUtils.getAllInterfaces(blockEntity.getClass()).stream()
                .filter(type -> type.getName().equals(OWNABLE))
                .findFirst()
                .orElse(null);
        if (ownable == null) return true;
        try {
            for (Class<?> parameter : new Class<?>[]{Entity.class, Player.class}) {
                try {
                    return (boolean) ownable.getMethod("isOwnedBy", parameter).invoke(blockEntity, player);
                } catch (NoSuchMethodException ignored) {
                }
            }
            throw new NoSuchMethodException(OWNABLE + ".isOwnedBy");
        } catch (ReflectiveOperationException | RuntimeException e) {
            LOGGER.warn("Could not check the SecurityCraft owner of the block at {}, it will not be captured", blockPos, e);
            return false;
        }
    }
}
