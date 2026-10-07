package capsule.plugins.claims;

import io.github.flemmli97.flan.api.ClaimHandler;
import io.github.flemmli97.flan.api.data.IPermissionContainer;
import io.github.flemmli97.flan.api.data.IPermissionStorage;
import io.github.flemmli97.flan.api.permission.BuiltinPermission;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import java.util.List;

/**
 * Flan protects boxes, claims and their sub-claims, with permission groups. Its API answers per position: the
 * permission container of a position is its claim, or the world outside claims. Here the player's BREAK permission
 * decides inside claims.
 */
class FlanAdapter implements ClaimAdapter {

    @Override
    public String name() {
        return "Flan";
    }

    @Override
    public List<Claim> claims(ServerLevel level, BoundingBox box, ServerPlayer player) {
        // ClaimHandler.canInteract asks the level of the player, here the level of the capture or deploy
        IPermissionStorage storage = ClaimHandler.getPermissionStorage(level);
        // below the world, no claim
        BlockPos unclaimed = new BlockPos(box.minX(), level.getMinBuildHeight() - 1, box.minZ());
        IPermissionContainer world = storage.getForPermissionCheck(unclaimed);
        if (storage.getForPermissionCheck(unclaimed) != world) throw new IllegalStateException("Flan's world outside claims is not one container");
        return ClaimAdapter.perPosition(box, pos -> {
            IPermissionContainer claim = storage.getForPermissionCheck(pos);
            return claim == world ? null : claim.canInteract(player, BuiltinPermission.BREAK, pos);
        });
    }
}
