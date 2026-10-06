package capsule.plugins.claims;

import capsule.platform.Services;
import capsule.plugins.claims.ClaimAdapter.Claim;
import com.mojang.authlib.GameProfile;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.longs.LongSets;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.GameProfileCache;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;

/**
 * Whether claim mods let a player capture or deploy blocks. Mods with an adapter are asked once per chunk or claim;
 * the positions they do not cover are probed through the loader's protection hook (a block placement event on
 * NeoForge, Common Protection API on Fabric), so that claim mods without adapter still protect them: each position up
 * to the largest survival capsule, once per chunk column above.
 */
public final class Claims {
    private static final Logger LOGGER = LogManager.getLogger(Claims.class);
    private static final List<ClaimAdapter> ADAPTERS = new CopyOnWriteArrayList<>();
    /**
     * Asks the claims for captures and deploys without a player (dispensers, capture bases placed before Capsule 9).
     */
    private static final GameProfile NOBODY = new GameProfile(UUID.fromString("9c0b9b7b-b356-41c0-93b2-4bb6afe1586c"), "[Capsule]");
    /**
     * Largest size probed per block: above, OP captures and deploys would take seconds, so they are probed per chunk column.
     */
    public static final int PER_BLOCK_MAX_SIZE = 31;
    private static boolean modsLoaded = false;

    private Claims() {
    }

    /**
     * Adds the claims of a protection mod Capsule has no adapter for.
     */
    public static void register(ClaimAdapter adapter) {
        ADAPTERS.add(adapter);
    }

    public static void unregister(ClaimAdapter adapter) {
        ADAPTERS.remove(adapter);
    }

    private interface Factory {
        ClaimAdapter create() throws ReflectiveOperationException;
    }

    private static List<ClaimAdapter> adapters() {
        if (!modsLoaded) {
            modsLoaded = true;
            load("openpartiesandclaims", OpenPartiesAndClaimsAdapter::new);
            load("flan", FlanAdapter::new);
            load("goml", GetOffMyLawnAdapter::new);
        }
        return ADAPTERS;
    }

    private static void load(String modId, Factory factory) {
        if (!Services.PLATFORM.isModLoaded(modId)) return;
        try {
            ADAPTERS.add(factory.create());
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            LOGGER.error("Captures and deploys ignore the claims of {}, its API was not found: {}", modId, e.toString());
        }
    }

    /**
     * The player a capture or deploy is checked as: the connected player with this id, or a fake player with their
     * profile when they are offline.
     */
    @Nullable
    public static ServerPlayer player(ServerLevel level, @Nullable UUID id) {
        if (id == null) return null;
        if (level.getPlayerByUUID(id) instanceof ServerPlayer player) return player;
        ServerPlayer player = level.getServer().getPlayerList().getPlayer(id);
        return player != null ? player : fakePlayer(level, id);
    }

    /**
     * A fake player with the profile of id, which gets no chat feedback.
     */
    @Nullable
    public static ServerPlayer fakePlayer(ServerLevel level, @Nullable UUID id) {
        if (id == null) return null;
        GameProfileCache profiles = level.getServer().getProfileCache();
        GameProfile profile = profiles == null ? null : profiles.get(id).orElse(null);
        return Services.PLATFORM.fakePlayer(level, profile != null ? profile : new GameProfile(id, "[Capsule]"));
    }

    /**
     * The positions the player may not change among positions. Without a player, no claimed position may be changed.
     */
    public static Predicate<BlockPos> denied(ServerLevel level, Collection<BlockPos> positions, @Nullable ServerPlayer player) {
        return BoundingBox.encapsulatingPositions(positions)
                .map(box -> denied(level, box, player))
                .orElse(pos -> false);
    }

    /**
     * The positions of box the player may not change. The adapters are asked here; testing a position outside their
     * claims probes it when box is at most PER_BLOCK_MAX_SIZE wide, else looks up its chunk column, probed here.
     */
    public static Predicate<BlockPos> denied(ServerLevel level, BoundingBox box, @Nullable ServerPlayer player) {
        ServerPlayer actor = player != null ? player : Services.PLATFORM.fakePlayer(level, NOBODY);
        // per chunk, the claims of each adapter
        Long2ObjectMap<List<List<Claim>>> claimsByChunk = new Long2ObjectOpenHashMap<>();
        for (ClaimAdapter adapter : adapters()) {
            List<Claim> claims;
            try {
                claims = adapter.claims(level, box, actor);
            } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
                ADAPTERS.remove(adapter);
                LOGGER.error("Captures and deploys now ignore the claims of {}, its query failed: {}", adapter.name(), e.toString());
                return pos -> true;
            }
            Long2ObjectMap<List<Claim>> adapterClaims = new Long2ObjectOpenHashMap<>();
            for (Claim claim : claims) {
                BoundingBox inBox = intersection(claim.box(), box);
                if (inBox == null) continue;
                for (int chunkX = inBox.minX() >> 4; chunkX <= inBox.maxX() >> 4; chunkX++) {
                    for (int chunkZ = inBox.minZ() >> 4; chunkZ <= inBox.maxZ() >> 4; chunkZ++) {
                        adapterClaims.computeIfAbsent(ChunkPos.asLong(chunkX, chunkZ), k -> new ArrayList<>()).add(claim);
                    }
                }
            }
            adapterClaims.long2ObjectEntrySet().forEach(e -> claimsByChunk.computeIfAbsent(e.getLongKey(), k -> new ArrayList<>()).add(e.getValue()));
        }

        boolean perBlock = Math.max(box.getXSpan(), Math.max(box.getYSpan(), box.getZSpan())) <= PER_BLOCK_MAX_SIZE;
        LongSet deniedColumns = perBlock ? LongSets.EMPTY_SET : deniedColumns(level, box, claimsByChunk, actor);

        return pos -> {
            long chunk = ChunkPos.asLong(pos.getX() >> 4, pos.getZ() >> 4);
            List<List<Claim>> chunkClaims = claimsByChunk.get(chunk);
            boolean claimed = false;
            if (chunkClaims != null) {
                for (List<Claim> claims : chunkClaims) {
                    Claim claim = lastContaining(claims, pos);
                    if (claim == null) continue;
                    if (!claim.allowed() || player == null) return true;
                    claimed = true;
                }
            }
            if (claimed) return false;
            return perBlock ? !Services.PLATFORM.canPlaceBlock(level, pos, actor) : deniedColumns.contains(chunk);
        };
    }

    /**
     * The chunk columns of box denied by mods without adapter, probed once each outside the adapter claims.
     */
    private static LongSet deniedColumns(ServerLevel level, BoundingBox box, Long2ObjectMap<List<List<Claim>>> claimsByChunk, ServerPlayer actor) {
        LongSet denied = new LongOpenHashSet();
        for (int chunkX = box.minX() >> 4; chunkX <= box.maxX() >> 4; chunkX++) {
            for (int chunkZ = box.minZ() >> 4; chunkZ <= box.maxZ() >> 4; chunkZ++) {
                long chunk = ChunkPos.asLong(chunkX, chunkZ);
                BoundingBox column = intersection(box, new BoundingBox(chunkX << 4, box.minY(), chunkZ << 4, (chunkX << 4) + 15, box.maxY(), (chunkZ << 4) + 15));
                BlockPos probe = unclaimedPosition(column, claimsByChunk.getOrDefault(chunk, List.of()));
                if (probe != null && !Services.PLATFORM.canPlaceBlock(level, probe, actor)) denied.add(chunk);
            }
        }
        return denied;
    }

    @Nullable
    private static Claim lastContaining(List<Claim> claims, BlockPos pos) {
        for (int i = claims.size() - 1; i >= 0; i--) {
            if (claims.get(i).box().isInside(pos)) return claims.get(i);
        }
        return null;
    }

    /**
     * The center or a corner of column outside every claim, where the claims of mods without adapter are probed.
     */
    @Nullable
    private static BlockPos unclaimedPosition(BoundingBox column, List<List<Claim>> claims) {
        List<BlockPos> candidates = new ArrayList<>();
        candidates.add(column.getCenter());
        for (int x : new int[]{column.minX(), column.maxX()}) {
            for (int y : new int[]{column.minY(), column.maxY()}) {
                for (int z : new int[]{column.minZ(), column.maxZ()}) {
                    candidates.add(new BlockPos(x, y, z));
                }
            }
        }
        return candidates.stream()
                .filter(pos -> claims.stream().allMatch(adapterClaims -> lastContaining(adapterClaims, pos) == null))
                .findFirst()
                .orElse(null);
    }

    @Nullable
    private static BoundingBox intersection(BoundingBox a, BoundingBox b) {
        if (!a.intersects(b)) return null;
        return new BoundingBox(Math.max(a.minX(), b.minX()), Math.max(a.minY(), b.minY()), Math.max(a.minZ(), b.minZ()),
                Math.min(a.maxX(), b.maxX()), Math.min(a.maxY(), b.maxY()), Math.min(a.maxZ(), b.maxZ()));
    }
}
