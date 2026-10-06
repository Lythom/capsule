package capsule.plugins.claims;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.server.ServerWorld;

import javax.annotation.Nullable;
import java.lang.reflect.Method;

/**
 * Flan 1.16.5 protects boxes: claims, and sub-claims inside them. It does not listen to the block placement event, so
 * it is asked per position through its storage: the claim at a position, then whether the player may break there (a
 * claim hands the positions of its sub-claims over to them).
 */
class FlanAdapter {
    private final Method storage;
    private final Method claimAt;
    private final Method canInteract;
    private final Object breakPermission;

    FlanAdapter() throws ReflectiveOperationException {
        Class<?> storageType = Class.forName("io.github.flemmli97.flan.claim.ClaimStorage");
        storage = storageType.getMethod("get", ServerWorld.class);
        claimAt = storageType.getMethod("getClaimAt", BlockPos.class);
        Class<?> claimType = Class.forName("io.github.flemmli97.flan.claim.Claim");
        Class<?> permissionType = Class.forName("io.github.flemmli97.flan.api.permission.ClaimPermission");
        canInteract = claimType.getMethod("canInteract", ServerPlayerEntity.class, permissionType, BlockPos.class, boolean.class);
        breakPermission = Class.forName("io.github.flemmli97.flan.api.permission.PermissionRegistry").getField("BREAK").get(null);
        if (!permissionType.isInstance(breakPermission)) throw new NoSuchFieldException("PermissionRegistry.BREAK");
    }

    String name() {
        return "Flan";
    }

    /**
     * The claims of level.
     */
    Object storage(ServerWorld level) throws ReflectiveOperationException {
        return storage.invoke(null, level);
    }

    /**
     * Whether the player may change pos, null if no claim of storage contains it. Without a player, no claimed position
     * may be changed.
     */
    @Nullable
    Boolean allowed(Object storage, BlockPos pos, @Nullable ServerPlayerEntity player) throws ReflectiveOperationException {
        Object claim = claimAt.invoke(storage, pos);
        if (claim == null) return null;
        return player != null && (boolean) canInteract.invoke(claim, player, breakPermission, pos, false);
    }
}
