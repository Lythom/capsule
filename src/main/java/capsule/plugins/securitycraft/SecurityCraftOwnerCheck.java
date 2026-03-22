package capsule.plugins.securitycraft;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.fml.ModList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SecurityCraftOwnerCheck {
    private static final Logger LOGGER = LogManager.getLogger(SecurityCraftOwnerCheck.class);

    public static boolean canTakeBlock(ServerLevel worldserver, BlockPos blockPos, Player player) {
        if (!ModList.get().isLoaded("securitycraft")) return true;
        // SecurityCraft integration disabled until a 1.21.1-compatible version is available.
        // The IOwnable interface check requires SecurityCraft as a compile-time dependency.
        LOGGER.debug("SecurityCraft is loaded but integration is not yet available for 1.21.1");
        return true;
    }
}
