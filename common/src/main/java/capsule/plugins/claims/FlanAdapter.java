package capsule.plugins.claims;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Flan protects boxes: claims, and sub-claims inside them. Its storage lists the claims of each chunk, and a claim
 * checks a permission (here BREAK) for a player.
 */
class FlanAdapter implements ClaimAdapter {
    private static final String[] BOUNDS = {"minX", "minY", "minZ", "maxX", "maxY", "maxZ"};
    private final Method storage;
    private final Method claimsAt;
    private final Method dimensions;
    private final Method subClaims;
    private final Method canInteract;
    private final Method[] bounds = new Method[BOUNDS.length];
    private final Object breakPermission;

    FlanAdapter() throws ReflectiveOperationException {
        Class<?> storageType = Class.forName("io.github.flemmli97.flan.claim.ClaimStorage");
        storage = storageType.getMethod("get", ServerLevel.class);
        claimsAt = storageType.getMethod("getClaimsAt", int.class, int.class);
        Class<?> claimType = Class.forName("io.github.flemmli97.flan.claim.Claim");
        dimensions = claimType.getMethod("getDimensions");
        subClaims = claimType.getMethod("getAllSubclaims");
        canInteract = claimType.getMethod("canInteract", ServerPlayer.class, ResourceLocation.class, BlockPos.class, boolean.class);
        Class<?> boxType = Class.forName("io.github.flemmli97.flan.claim.ClaimBox");
        for (int i = 0; i < BOUNDS.length; i++) bounds[i] = boxType.getMethod(BOUNDS[i]);
        breakPermission = Class.forName("io.github.flemmli97.flan.api.permission.BuiltinPermission").getField("BREAK").get(null);
    }

    @Override
    public String name() {
        return "Flan";
    }

    @Override
    public List<Claim> claims(ServerLevel level, BoundingBox box, ServerPlayer player) throws ReflectiveOperationException {
        Object claimStorage = storage.invoke(null, level);
        Set<Object> found = new LinkedHashSet<>();
        for (int chunkX = box.minX() >> 4; chunkX <= box.maxX() >> 4; chunkX++) {
            for (int chunkZ = box.minZ() >> 4; chunkZ <= box.maxZ() >> 4; chunkZ++) {
                found.addAll((List<?>) claimsAt.invoke(claimStorage, chunkX, chunkZ));
            }
        }
        List<Claim> claims = new ArrayList<>();
        for (Object claim : found) {
            if (!add(claims, claim, box, player)) continue;
            for (Object subClaim : (List<?>) subClaims.invoke(claim)) add(claims, subClaim, box, player);
        }
        return claims;
    }

    private boolean add(List<Claim> claims, Object claim, BoundingBox box, ServerPlayer player) throws ReflectiveOperationException {
        Object claimBox = dimensions.invoke(claim);
        int[] b = new int[BOUNDS.length];
        for (int i = 0; i < BOUNDS.length; i++) b[i] = (int) bounds[i].invoke(claimBox);
        BoundingBox claimed = new BoundingBox(b[0], b[1], b[2], b[3], b[4], b[5]);
        if (!claimed.intersects(box)) return false;
        // a claim hands the positions inside its sub-claims over to them: asked outside, it answers for itself
        BlockPos outside = new BlockPos(claimed.minX() - 1, claimed.minY(), claimed.minZ());
        claims.add(new Claim(claimed, (boolean) canInteract.invoke(claim, player, breakPermission, outside, false)));
        return true;
    }
}
