package capsule.plugins.claims;

import com.mojang.authlib.GameProfile;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.server.management.PlayerProfileCache;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MutableBoundingBox;
import net.minecraft.util.text.TranslationTextComponent;
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
import java.util.BitSet;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * Whether claim mods let a player capture or deploy blocks. Flan, which does not listen to the block placement event on
 * 1.16.5, is asked per position through its API; the other positions are probed with a block placement event, so that
 * claim mods without adapter protect them: every block up to {@link #PER_BLOCK_MAX_SIZE}, once per chunk column above.
 * When Flan is loaded but cannot be checked, captures and deploys are refused.
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
    /**
     * Flan's name and version when it is loaded without the API its adapter needs.
     */
    @Nullable
    private static String unusable = null;
    private static boolean failureReported = false;

    private Claims() {
    }

    /**
     * Loads the Flan adapter when Flan is present, once, when the server starts.
     */
    public static void loadAdapters() {
        if (flanLoaded) return;
        flanLoaded = true;
        if (!ModList.get().isLoaded("flan")) return;
        try {
            flan = new FlanAdapter();
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            unusable = ModList.get().getModContainerById("flan")
                    .map(mod -> mod.getModInfo().getDisplayName() + " " + mod.getModInfo().getVersion())
                    .orElse("flan");
            LOGGER.error("Captures and deploys are refused: Capsule cannot check the claims of {}, its API was not found ({}). Please report this incompatibility.",
                    unusable, e.toString());
        }
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
     * Flan and the chunk columns of large boxes are asked here, before the capture or deploy changes the world; the
     * other positions are probed when tested.
     *
     * @return null when Flan cannot be checked: the operation is refused, the player is told
     */
    @Nullable
    public static Predicate<BlockPos> denied(ServerWorld level, Collection<BlockPos> positions, @Nullable ServerPlayerEntity player) {
        if (positions.isEmpty()) return pos -> false;
        if (unusable != null) return refused(player, unusable);
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
        // the positions inside Flan claims and those the player may change, by index in box
        BitSet flanClaimed = new BitSet();
        BitSet flanAllowed = new BitSet();
        List<BlockPos> columnProbes;
        try {
            Object flanClaims = flan == null ? null : flan.storage(level);
            if (flanClaims != null) {
                for (BlockPos pos : positions) {
                    Boolean allowed = flan.allowed(flanClaims, pos, player);
                    if (allowed == null) continue;
                    int i = index(box, pos);
                    flanClaimed.set(i);
                    flanAllowed.set(i, allowed);
                }
            }
            columnProbes = perBlock ? new ArrayList<>() : columnProbes(box, flanClaims);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            if (!failureReported) {
                failureReported = true;
                LOGGER.error("Captures and deploys are refused while Capsule cannot check the claims of Flan. Please report this incompatibility.", e);
            }
            return refused(player, flan.name());
        }
        LongSet deniedColumns = new LongOpenHashSet();
        for (BlockPos probe : columnProbes) {
            if (!canPlaceBlock(level, probe, actor)) deniedColumns.add(ChunkPos.asLong(probe.getX() >> 4, probe.getZ() >> 4));
        }
        return pos -> {
            int i = index(box, pos);
            if (flanClaimed.get(i)) return !flanAllowed.get(i);
            if (perBlock) return !canPlaceBlock(level, pos, actor);
            return deniedColumns.contains(ChunkPos.asLong(pos.getX() >> 4, pos.getZ() >> 4));
        };
    }

    /**
     * The index of pos, inside box, in a bit set of box.
     */
    private static int index(MutableBoundingBox box, BlockPos pos) {
        return ((pos.getX() - box.x0) * box.getYSpan() + pos.getY() - box.y0) * box.getZSpan() + pos.getZ() - box.z0;
    }

    /**
     * Where the claims of mods without adapter are probed in each chunk column of box: its center or a corner that no
     * Flan claim contains, none if Flan claims them all.
     */
    private static List<BlockPos> columnProbes(MutableBoundingBox box, @Nullable Object flanClaims) throws ReflectiveOperationException {
        List<BlockPos> probes = new ArrayList<>();
        for (int chunkX = box.x0 >> 4; chunkX <= box.x1 >> 4; chunkX++) {
            for (int chunkZ = box.z0 >> 4; chunkZ <= box.z1 >> 4; chunkZ++) {
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
                    probes.add(candidate);
                    break;
                }
            }
        }
        return probes;
    }

    /**
     * Refuses a capture or deploy because the claims of mod cannot be checked, telling the player.
     */
    @Nullable
    private static Predicate<BlockPos> refused(@Nullable ServerPlayerEntity player, String mod) {
        if (player != null) player.sendMessage(new TranslationTextComponent("capsule.error.claimCheckFailed", mod), Util.NIL_UUID);
        return null;
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
