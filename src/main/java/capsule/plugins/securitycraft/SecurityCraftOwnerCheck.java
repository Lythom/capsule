package capsule.plugins.securitycraft;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.fml.ModList;

/**
 * SecurityCraft blocks can only be taken by their owner.
 */
public class SecurityCraftOwnerCheck {

    public static boolean canTakeBlock(ServerLevel worldserver, BlockPos blockPos, Player player) {
        return !ModList.get().isLoaded("securitycraft") || Owners.canTake(worldserver, blockPos, player);
    }
}
