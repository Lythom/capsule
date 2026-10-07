package capsule.plugins.securitycraft;

import net.geforcemods.securitycraft.api.IOwnable;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.fml.ModList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * SecurityCraft blocks can only be taken by their owner. A block is protected if SecurityCraft's API cannot be asked.
 */
public class SecurityCraftOwnerCheck {
    private static final Logger LOGGER = LogManager.getLogger(SecurityCraftOwnerCheck.class);
    private static boolean warned;

    public static boolean canTakeBlock(ServerWorld worldserver, BlockPos blockPos, PlayerEntity player) {
        if (!ModList.get().isLoaded("securitycraft")) return true;
        try {
            TileEntity tileEntity = worldserver.getBlockEntity(blockPos);
            return !(tileEntity instanceof IOwnable) || ((IOwnable) tileEntity).getOwner().isOwner(player);
        } catch (RuntimeException | LinkageError e) {
            if (!warned) LOGGER.warn("Could not check the SecurityCraft owner of the block at {}, blocks whose owner cannot be checked are not captured", blockPos, e);
            warned = true;
            return false;
        }
    }
}
