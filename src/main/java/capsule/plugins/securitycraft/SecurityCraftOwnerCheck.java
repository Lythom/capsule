package capsule.plugins.securitycraft;

import net.geforcemods.securitycraft.api.IOwnable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.fml.ModList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * SecurityCraft blocks can only be taken by their owner. A block is protected if SecurityCraft's API cannot be asked.
 */
public class SecurityCraftOwnerCheck {
    private static final Logger LOGGER = LogManager.getLogger(SecurityCraftOwnerCheck.class);
    private static boolean warned;

    public static boolean canTakeBlock(ServerLevel worldserver, BlockPos blockPos, Player player) {
        if (!ModList.get().isLoaded("securitycraft")) return true;
        try {
            return !(worldserver.getBlockEntity(blockPos) instanceof IOwnable ownable) || ownable.getOwner().isOwner(player);
        } catch (RuntimeException | LinkageError e) {
            if (!warned) LOGGER.warn("Could not check the SecurityCraft owner of the block at {}, blocks whose owner cannot be checked are not captured", blockPos, e);
            warned = true;
            return false;
        }
    }
}
