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

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * Whether claim mods let a player capture or deploy blocks, asked with a block placement event: for every block up to
 * {@link #PER_BLOCK_MAX_SIZE}, once per chunk column above.
 */
public final class Claims {
    /**
     * Asks the claims for captures and deploys without a player (dispensers, capture bases placed before this version).
     */
    private static final GameProfile NOBODY = new GameProfile(UUID.fromString("9c0b9b7b-b356-41c0-93b2-4bb6afe1586c"), "[Capsule]");
    /**
     * Captures and deploys up to this size (the largest upgraded capsule) probe every block, larger ones (OP capsules)
     * one block per chunk column: one placement event per block of a 255 capsule takes seconds.
     */
    public static final int PER_BLOCK_MAX_SIZE = 31;

    private Claims() {
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
        Long2BooleanMap deniedChunks = new Long2BooleanOpenHashMap();
        return pos -> {
            if (perBlock) return !canPlaceBlock(level, pos, actor);
            long chunk = ChunkPos.asLong(pos.getX() >> 4, pos.getZ() >> 4);
            if (!deniedChunks.containsKey(chunk)) deniedChunks.put(chunk, columnDenied(level, box, pos, actor));
            return deniedChunks.get(chunk);
        };
    }

    /**
     * Whether claim mods deny the chunk column of pos, probed at the center of the column within box.
     */
    private static boolean columnDenied(ServerWorld level, MutableBoundingBox box, BlockPos pos, ServerPlayerEntity actor) {
        int chunkX = pos.getX() >> 4;
        int chunkZ = pos.getZ() >> 4;
        MutableBoundingBox column = new MutableBoundingBox(Math.max(box.x0, chunkX << 4), box.y0, Math.max(box.z0, chunkZ << 4),
                Math.min(box.x1, (chunkX << 4) + 15), box.y1, Math.min(box.z1, (chunkZ << 4) + 15));
        return !canPlaceBlock(level, new BlockPos(column.getCenter()), actor);
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
