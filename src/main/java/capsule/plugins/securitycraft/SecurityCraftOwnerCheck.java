package capsule.plugins.securitycraft;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.fml.ModList;
import net.geforcemods.securitycraft.api.IOwnable;

public class SecurityCraftOwnerCheck {
    public static boolean canTakeBlock(ServerLevel worldserver, BlockPos blockPos, Player player) {
        if (!ModList.get().isLoaded("securitycraft")) return true;
        BlockEntity BlockEntity = worldserver.getBlockEntity(blockPos);
        // if not an IOwnable, SecurityCraft should not prevent taking
        if (!(BlockEntity instanceof IOwnable)) return true;
        // can take the block if owner
        return ((IOwnable) BlockEntity).getOwner().isOwner(player);
    }
}
