package capsule.plugins.securitycraft;

import net.geforcemods.securitycraft.api.IOwnable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.fml.ModList;

public class SecurityCraftOwnerCheck {
    public static boolean canTakeBlock(ServerLevel worldserver, BlockPos blockPos, Player player) {
        if (!ModList.get().isLoaded("securitycraft")) return true;
        BlockEntity tileEntity = worldserver.getBlockEntity(blockPos);
        // if not an IOwnable, SecurityCraft should not prevent taking
        if (!(tileEntity instanceof IOwnable)) return true;
        // can take the block if owner
        return ((IOwnable) tileEntity).getOwner().isOwner(player);
    }
}
