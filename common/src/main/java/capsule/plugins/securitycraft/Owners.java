package capsule.plugins.securitycraft;

import net.geforcemods.securitycraft.api.IOwnable;
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
    private static boolean warned;

    static boolean canTake(ServerLevel level, BlockPos pos, Player player) {
        try {
            return !(level.getBlockEntity(pos) instanceof IOwnable ownable) || ownable.isOwnedBy(player);
        } catch (RuntimeException | LinkageError e) {
            if (!warned) LOGGER.warn("Could not check the SecurityCraft owner of the block at {}, blocks whose owner cannot be checked are not captured", pos, e);
            warned = true;
            return false;
        }
    }
}
