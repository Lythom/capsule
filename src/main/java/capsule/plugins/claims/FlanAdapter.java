package capsule.plugins.claims;

import io.github.flemmli97.flan.api.ClaimHandler;
import io.github.flemmli97.flan.api.data.IPermissionContainer;
import io.github.flemmli97.flan.api.data.IPermissionStorage;
import io.github.flemmli97.flan.api.permission.PermissionRegistry;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.server.ServerWorld;

import javax.annotation.Nullable;
import java.util.function.Function;

/**
 * Flan 1.16.5 protects boxes: claims, and sub-claims inside them. It does not listen to the block placement event, so
 * it is asked through its API per position: the permission container of a position is its claim, or the world outside
 * claims. Inside claims the player's BREAK permission decides.
 */
class FlanAdapter {

    String name() {
        return "Flan";
    }

    /**
     * Whether the player may change a position of level, null if no claim contains it. Without a player, no claimed
     * position may be changed.
     */
    Function<BlockPos, Boolean> claims(ServerWorld level, @Nullable ServerPlayerEntity player) {
        IPermissionStorage storage = ClaimHandler.getPermissionStorage(level);
        // below the world, no claim
        BlockPos unclaimed = new BlockPos(0, -1, 0);
        IPermissionContainer world = storage.getForPermissionCheck(unclaimed);
        if (storage.getForPermissionCheck(unclaimed) != world) throw new IllegalStateException("Flan's world outside claims is not one container");
        return pos -> {
            IPermissionContainer claim = storage.getForPermissionCheck(pos);
            if (claim == world) return null;
            return player != null && claim.canInteract(player, PermissionRegistry.BREAK, pos);
        };
    }
}
