package capsule.plugins.securitycraft;

import net.geforcemods.securitycraft.api.IOwnable;
import net.geforcemods.securitycraft.api.Owner;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Asks SecurityCraft's API (IOwnable) who owns a block: loaded only when SecurityCraft is. A SecurityCraft block is
 * protected if the call fails.
 */
class Owners {
    private static final Logger LOGGER = LogManager.getLogger(Owners.class);
    // SecurityCraft before v1.9.9 has no isOwnedBy(Entity), it is asked with an Owner
    private static boolean legacy;
    private static boolean warned;

    static boolean canTake(ServerLevel level, BlockPos pos, Player player) {
        try {
            if (!(level.getBlockEntity(pos) instanceof IOwnable ownable)) return true;
            if (!legacy) {
                try {
                    return ownable.isOwnedBy(player);
                } catch (NoSuchMethodError e) {
                    legacy = true;
                }
            }
            return ownable.isOwnedBy(new Owner(player));
        } catch (RuntimeException | LinkageError e) {
            if (!warned) LOGGER.warn("Could not check the SecurityCraft owner of the block at {}, blocks whose owner cannot be checked are not captured", pos, e);
            warned = true;
            return false;
        }
    }
}
