package capsule.plugins.claims;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Tells which claims of one protection mod intersect a box and whether a player may change their blocks. An adapter
 * queries its mod once per chunk or claim of the box; a mod answering per position only is asked like the generic probe.
 */
public interface ClaimAdapter {

    /**
     * The mod, as named in the logs.
     */
    String name();

    /**
     * The claims intersecting box. Where claims overlap, the last one decides (sub-claims come after their claim).
     */
    List<Claim> claims(ServerLevel level, BoundingBox box, ServerPlayer player);

    record Claim(BoundingBox box, boolean allowed) {
    }

    interface ChunkQuery {
        /**
         * Whether the player may change the chunk, null if it is not claimed.
         */
        @Nullable
        Boolean allowed(int chunkX, int chunkZ);
    }

    /**
     * The claims of a mod protecting whole chunk columns, one query per chunk of box.
     */
    static List<Claim> perChunk(ServerLevel level, BoundingBox box, ChunkQuery query) {
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

    interface PositionQuery {
        /**
         * Whether the player may change pos, null if it is not claimed.
         */
        @Nullable
        Boolean allowed(BlockPos pos);
    }

    /**
     * The claims of a mod answering per position, asked like the generic probe: each position of box up to
     * {@link Claims#PER_BLOCK_MAX_SIZE} (one claim per run of the same answer along y), above once per chunk column of
     * box, at its center.
     */
    static List<Claim> perPosition(BoundingBox box, PositionQuery query) {
        List<Claim> claims = new ArrayList<>();
        if (Claims.perBlock(box)) {
            for (int x = box.minX(); x <= box.maxX(); x++) {
                for (int z = box.minZ(); z <= box.maxZ(); z++) {
                    Boolean run = null;
                    int start = box.minY();
                    for (int y = box.minY(); y <= box.maxY() + 1; y++) {
                        Boolean allowed = y <= box.maxY() ? query.allowed(new BlockPos(x, y, z)) : null;
                        if (Objects.equals(allowed, run)) continue;
                        if (run != null) claims.add(new Claim(new BoundingBox(x, start, z, x, y - 1, z), run));
                        run = allowed;
                        start = y;
                    }
                }
            }
            return claims;
        }
        for (int chunkX = box.minX() >> 4; chunkX <= box.maxX() >> 4; chunkX++) {
            for (int chunkZ = box.minZ() >> 4; chunkZ <= box.maxZ() >> 4; chunkZ++) {
                BoundingBox column = Claims.intersection(box, new BoundingBox(chunkX << 4, box.minY(), chunkZ << 4, (chunkX << 4) + 15, box.maxY(), (chunkZ << 4) + 15));
                Boolean allowed = query.allowed(column.getCenter());
                if (allowed != null) claims.add(new Claim(column, allowed));
            }
        }
        return claims;
    }
}
