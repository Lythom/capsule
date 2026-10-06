package capsule.plugins.claims;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Tells which claims of one protection mod intersect a box and whether a player may change their blocks. An adapter
 * queries its mod once per chunk or claim of the box, never per block.
 */
public interface ClaimAdapter {

    /**
     * The mod, as named in the logs.
     */
    String name();

    /**
     * The claims intersecting box. Where claims overlap, the last one decides (sub-claims come after their claim).
     */
    List<Claim> claims(ServerLevel level, BoundingBox box, ServerPlayer player) throws ReflectiveOperationException;

    record Claim(BoundingBox box, boolean allowed) {
    }

    interface ChunkQuery {
        /**
         * Whether the player may change the chunk, null if it is not claimed.
         */
        @Nullable
        Boolean allowed(int chunkX, int chunkZ) throws ReflectiveOperationException;
    }

    /**
     * The claims of a mod protecting whole chunk columns, one query per chunk of box.
     */
    static List<Claim> perChunk(ServerLevel level, BoundingBox box, ChunkQuery query) throws ReflectiveOperationException {
        List<Claim> claims = new ArrayList<>();
        for (int chunkX = box.minX() >> 4; chunkX <= box.maxX() >> 4; chunkX++) {
            for (int chunkZ = box.minZ() >> 4; chunkZ <= box.maxZ() >> 4; chunkZ++) {
                Boolean allowed = query.allowed(chunkX, chunkZ);
                if (allowed != null) {
                    BoundingBox column = new BoundingBox(chunkX << 4, level.getMinBuildHeight(), chunkZ << 4,
                            (chunkX << 4) + 15, level.getMaxBuildHeight() - 1, (chunkZ << 4) + 15);
                    claims.add(new Claim(column, allowed));
                }
            }
        }
        return claims;
    }
}
