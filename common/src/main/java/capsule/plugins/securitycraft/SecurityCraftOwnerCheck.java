package capsule.plugins.securitycraft;

import capsule.platform.Services;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;

/**
 * SecurityCraft blocks can only be taken by their owner.
 */
public class SecurityCraftOwnerCheck {

    public static boolean canTakeBlock(ServerLevel worldserver, BlockPos blockPos, Player player) {
        return !Services.PLATFORM.isModLoaded("securitycraft") || Owners.canTake(worldserver, blockPos, player);
    }
}
