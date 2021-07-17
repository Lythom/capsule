package capsule.plugins.securitycraft;

import net.geforcemods.securitycraft.api.IOwnable;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.fml.ModList;

public class SecurityCraftOwnerCheck {
    public static boolean canTakeBlock(ServerWorld worldserver, BlockPos blockPos, PlayerEntity player) {
        if (!ModList.get().isLoaded("securitycraft")) return true;
        TileEntity tileEntity = worldserver.getBlockEntity(blockPos);
        // if not an IOwnable, SecurityCraft should not prevent taking
        if (!(tileEntity instanceof IOwnable)) return true;
        // can take the block if owner
        return ((IOwnable) tileEntity).getOwner().isOwner(player);
    }
}
