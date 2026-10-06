package capsule.plugins.claims;

import com.mojang.authlib.GameProfile;
import it.unimi.dsi.fastutil.longs.Long2BooleanMap;
import it.unimi.dsi.fastutil.longs.Long2BooleanOpenHashMap;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.server.management.PlayerProfileCache;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MutableBoundingBox;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.ModList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * Whether claim mods let a player capture or deploy blocks. Flan, which does not listen to the block placement event on
 * 1.16.5, is asked per position through its API; the other positions are probed with a block placement event, so that
 * claim mods without adapter protect them: every block up to {@link #PER_BLOCK_MAX_SIZE}, once per chunk column above.
 */
public final class Claims {
    private static final Logger LOGGER = LogManager.getLogger(Claims.class);
    /**
     * Asks the claims for captures and deploys without a player (dispensers, capture bases placed before this version).
     */
    private static final GameProfile NOBODY = new GameProfile(UUID.fromString("9c0b9b7b-b356-41c0-93b2-4bb6afe1586c"), "[Capsule]");
    /**
     * Captures and deploys up to this size (the largest upgraded capsule) probe every block, larger ones (OP capsules)
     * one block per chunk column: one placement event per block of a 255 capsule takes seconds.
     */
    public static final int PER_BLOCK_MAX_SIZE = 31;
    private static boolean flanLoaded = false;
    @Nullable
    private static FlanAdapter flan = null;

    private Claims() {
    }

    @Nullable
    private static FlanAdapter flan() {
        if (!flanLoaded) {
            flanLoaded = true;
            if (ModList.get().isLoaded("flan")) {
                try {
                    flan = new FlanAdapter();
                } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
                    LOGGER.error("Captures and deploys ignore the claims of Flan, its API was not found: {}", e.toString());
                }
            }
        }
        return flan;
    }

    /**
     * The player a capture or deploy is checked as: the connected player with this id, or a fake player with their
     * profile when they are offline.
     */
    @Nullable
    public static ServerPlayerEntity player(ServerWorld level, @Nullable UUID id) {
        if (id == null) return null;
        PlayerEntity player = level.getPlayerByUUID(id);
        if (player instanceof ServerPlayerEntity) return (ServerPlayerEntity) player;
        ServerPlayerEntity connected = level.getServer().getPlayerList().getPlayer(id);
        return connected != null ? connected : fakePlayer(level, id);
    }

    /**
     * The player the claims are asked for: player, or without a player an anonymous fake player that no claim lists as
     * a member, so that nobody may change a claimed position.
     */
    public static ServerPlayerEntity actor(ServerWorld level, @Nullable ServerPlayerEntity player) {
        return player != null ? player : FakePlayerFactory.get(level, NOBODY);
    }

    /**
     * A fake player with the profile of id, which gets no chat feedback.
     */
    @Nullable
    public static ServerPlayerEntity fakePlayer(ServerWorld level, @Nullable UUID id) {
        if (id == null) return null;
        PlayerProfileCache profiles = level.getServer().getProfileCache();
        GameProfile profile = profiles == null ? null : profiles.get(id);
        return FakePlayerFactory.get(level, profile != null ? profile : new GameProfile(id, "[Capsule]"));
    }

    /**
     * The positions among positions the player may not change. Without a player, no claimed position may be changed.
     * Each position is asked when tested, the chunk columns of large boxes once.
     */
    public static Predicate<BlockPos> denied(ServerWorld level, Collection<BlockPos> positions, @Nullable ServerPlayerEntity player) {
        if (positions.isEmpty()) return pos -> false;
        int[] min = {Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE};
        int[] max = {Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE};
        for (BlockPos pos : positions) {
            int[] coordinates = {pos.getX(), pos.getY(), pos.getZ()};
            for (int i = 0; i < 3; i++) {
                min[i] = Math.min(min[i], coordinates[i]);
                max[i] = Math.max(max[i], coordinates[i]);
            }
        }
        MutableBoundingBox box = new MutableBoundingBox(min[0], min[1], min[2], max[0], max[1], max[2]);
        ServerPlayerEntity actor = actor(level, player);
        boolean perBlock = Math.max(box.getXSpan(), Math.max(box.getYSpan(), box.getZSpan())) <= PER_BLOCK_MAX_SIZE;
        FlanAdapter adapter = flan();
        Object flanClaims;
        try {
            flanClaims = adapter == null ? null : adapter.storage(level);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            return failed(adapter, e);
        }
        Long2BooleanMap deniedChunks = new Long2BooleanOpenHashMap();
        return new Predicate<BlockPos>() {
            private boolean failed = false;

            @Override
            public boolean test(BlockPos pos) {
                if (failed) return true;
                try {
                    Boolean allowed = flanClaims == null ? null : adapter.allowed(flanClaims, pos, player);
                    if (allowed != null) return !allowed;
                    if (perBlock) return !canPlaceBlock(level, pos, actor);
                    long chunk = ChunkPos.asLong(pos.getX() >> 4, pos.getZ() >> 4);
                    if (!deniedChunks.containsKey(chunk)) {
                        deniedChunks.put(chunk, columnDenied(level, box, pos, actor, adapter, flanClaims));
                    }
                    return deniedChunks.get(chunk);
                } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
                    failed(adapter, e);
                    failed = true;
                    return true;
                }
            }
        };
    }

    /**
     * Whether the claims of mods without adapter deny the chunk column of pos, probed at the center or a corner of the
     * column within box that no Flan claim contains.
     */
    private static boolean columnDenied(ServerWorld level, MutableBoundingBox box, BlockPos pos, ServerPlayerEntity actor,
                                        @Nullable FlanAdapter flan, @Nullable Object flanClaims) throws ReflectiveOperationException {
        int chunkX = pos.getX() >> 4;
        int chunkZ = pos.getZ() >> 4;
        MutableBoundingBox column = new MutableBoundingBox(Math.max(box.x0, chunkX << 4), box.y0, Math.max(box.z0, chunkZ << 4),
                Math.min(box.x1, (chunkX << 4) + 15), box.y1, Math.min(box.z1, (chunkZ << 4) + 15));
        List<BlockPos> candidates = new ArrayList<>();
        candidates.add(new BlockPos(column.getCenter()));
        for (int x : new int[]{column.x0, column.x1}) {
            for (int y : new int[]{column.y0, column.y1}) {
                for (int z : new int[]{column.z0, column.z1}) {
                    candidates.add(new BlockPos(x, y, z));
                }
            }
        }
        for (BlockPos candidate : candidates) {
            if (flanClaims != null && flan.allowed(flanClaims, candidate, null) != null) continue;
            return !canPlaceBlock(level, candidate, actor);
        }
        return false;
    }

    private static Predicate<BlockPos> failed(@Nullable FlanAdapter adapter, Throwable e) {
        if (adapter != null && flan == adapter) {
            flan = null;
            LOGGER.error("Captures and deploys now ignore the claims of {}, its query failed: {}", adapter.name(), e.toString());
        }
        return pos -> true;
    }

    /**
     * Whether protection mods let the player place a block at pos, asked with a dirt placement event.
     */
    public static boolean canPlaceBlock(ServerWorld level, BlockPos pos, PlayerEntity player) {
        BlockSnapshot snapshot = BlockSnapshot.create(level.dimension(), level, pos);
        BlockEvent.EntityPlaceEvent event = new BlockEvent.EntityPlaceEvent(snapshot, Blocks.DIRT.defaultBlockState(), player);
        MinecraftForge.EVENT_BUS.post(event);
        return !event.isCanceled();
    }
}
