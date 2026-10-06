package capsule.dev;

import capsule.CapsuleMod;
import capsule.plugins.claims.Claims;
import com.mojang.authlib.GameProfile;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.util.ReferenceCountUtil;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.PacketDirection;
import net.minecraft.network.play.ServerPlayNetHandler;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.PlayerInteractionManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.server.FMLServerStartedEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Method;
import java.util.AbstractCollection;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * Cost of the claim probe (the dirt placement event of {@link Claims#canPlaceBlock}) asked for every block or once per
 * chunk column of a capture of 3, 11, 31 and 255, by a stranger, and of the check captures and deploys do
 * ({@link Claims#denied}: the Flan adapter per position, the probe per block up to 31 and per chunk column above),
 * outside claims and, when Flan is loaded (-PmodCompat), inside a Flan admin claim covering the box. Runs once the dev
 * server started with -Dcapsule.claimBenchmark=true (runServer -PclaimBenchmark), logs the median times as "claim probe
 * benchmark", then stops the server. Left out of the mod jar.
 */
@Mod.EventBusSubscriber(modid = CapsuleMod.MODID)
public class ClaimProbeBenchmark {
    private static final Logger LOGGER = LogManager.getLogger(ClaimProbeBenchmark.class);
    private static final int[] SIZES = {3, 11, 31, 255};

    @SubscribeEvent
    public static void serverStarted(FMLServerStartedEvent event) {
        if (!Boolean.getBoolean("capsule.claimBenchmark")) return;
        MinecraftServer server = event.getServer();
        ServerWorld level = server.overworld();
        try {
            ServerPlayerEntity stranger = stranger(server, level);
            BlockPos spawn = level.getSharedSpawnPos();
            BlockPos min = new BlockPos(spawn.getX() + 3, 1, spawn.getZ() + 5);
            int largest = SIZES[SIZES.length - 1];
            // loads the chunks and the probe code before measuring
            perColumn(level, min, largest, stranger);
            perBlock(level, min, 31, stranger);
            check(level, min, 31, stranger);
            measure(level, "no claim", min, stranger);

            if (ModList.get().isLoaded("flan")) {
                Runnable unclaim = flanAdminClaim(level, min, largest, stranger);
                try {
                    measure(level, "Flan admin claim", min, stranger);
                } finally {
                    unclaim.run();
                }
            }
        } catch (Exception e) {
            LOGGER.error("claim probe benchmark failed", e);
        } finally {
            server.halt(false);
        }
    }

    private static void measure(ServerWorld level, String scenario, BlockPos min, ServerPlayerEntity player) {
        for (int size : SIZES) {
            int runs = size < 255 ? 7 : 3;
            long[] blockTimes = new long[runs];
            long[] columnTimes = new long[runs];
            long[] checkTimes = new long[runs];
            int deniedBlocks = 0;
            int deniedColumns = 0;
            int deniedChecked = 0;
            for (int run = 0; run < runs; run++) {
                long start = System.nanoTime();
                deniedBlocks = perBlock(level, min, size, player);
                blockTimes[run] = System.nanoTime() - start;
                start = System.nanoTime();
                deniedColumns = perColumn(level, min, size, player);
                columnTimes[run] = System.nanoTime() - start;
                start = System.nanoTime();
                deniedChecked = check(level, min, size, player);
                checkTimes[run] = System.nanoTime() - start;
            }
            int maxX = min.getX() + size - 1;
            int maxZ = min.getZ() + size - 1;
            int columns = ((maxX >> 4) - (min.getX() >> 4) + 1) * ((maxZ >> 4) - (min.getZ() >> 4) + 1);
            LOGGER.info("claim probe benchmark | Forge 1.16.5 | {} | size {} | per block: {} probes, {} denied, {} ms | per chunk column: {} probes, {} denied, {} ms | Claims.denied: {} denied, {} ms | median of {}",
                    scenario, size, (long) size * size * size, deniedBlocks, median(blockTimes), columns, deniedColumns, median(columnTimes),
                    deniedChecked, median(checkTimes), runs);
        }
    }

    private static int perBlock(ServerWorld level, BlockPos min, int size, PlayerEntity player) {
        int denied = 0;
        for (BlockPos pos : BlockPos.betweenClosed(min, min.offset(size - 1, size - 1, size - 1))) {
            if (!Claims.canPlaceBlock(level, pos, player)) denied++;
        }
        return denied;
    }

    private static int perColumn(ServerWorld level, BlockPos min, int size, PlayerEntity player) {
        int maxX = min.getX() + size - 1;
        int maxZ = min.getZ() + size - 1;
        int y = min.getY() + size / 2;
        int denied = 0;
        for (int chunkX = min.getX() >> 4; chunkX <= maxX >> 4; chunkX++) {
            for (int chunkZ = min.getZ() >> 4; chunkZ <= maxZ >> 4; chunkZ++) {
                int x = Math.min(Math.max((chunkX << 4) + 8, min.getX()), maxX);
                int z = Math.min(Math.max((chunkZ << 4) + 8, min.getZ()), maxZ);
                if (!Claims.canPlaceBlock(level, new BlockPos(x, y, z), player)) denied++;
            }
        }
        return denied;
    }

    /**
     * The check of a capture of every block of the box, as StructureSaver does it.
     */
    private static int check(ServerWorld level, BlockPos min, int size, ServerPlayerEntity player) {
        Collection<BlockPos> positions = box(min, size);
        Predicate<BlockPos> claimed = Claims.denied(level, positions, player);
        int denied = 0;
        for (BlockPos pos : positions) {
            if (claimed.test(pos)) denied++;
        }
        return denied;
    }

    /**
     * The positions of the box, without keeping them.
     */
    private static Collection<BlockPos> box(BlockPos min, int size) {
        return new AbstractCollection<BlockPos>() {
            @Override
            public Iterator<BlockPos> iterator() {
                return BlockPos.betweenClosed(min, min.offset(size - 1, size - 1, size - 1)).iterator();
            }

            @Override
            public int size() {
                return size * size * size;
            }
        };
    }

    /**
     * A player who is not connected and owns no claim. The packets sent to them are dropped.
     */
    private static ServerPlayerEntity stranger(MinecraftServer server, ServerWorld level) {
        ServerPlayerEntity player = new ServerPlayerEntity(server, level, new GameProfile(UUID.randomUUID(), "stranger"), new PlayerInteractionManager(level));
        NetworkManager connection = new NetworkManager(PacketDirection.SERVERBOUND);
        new EmbeddedChannel(new ChannelOutboundHandlerAdapter() {
            @Override
            public void write(ChannelHandlerContext context, Object message, ChannelPromise promise) {
                ReferenceCountUtil.release(message);
                promise.setSuccess();
            }
        }, connection);
        new ServerPlayNetHandler(server, connection, player);
        return player;
    }

    /**
     * A Flan admin claim (no owner) over the columns of the box, until the returned task runs. Flan is used through
     * reflection: it is only on the benchmark's runtime classpath.
     */
    private static Runnable flanAdminClaim(ServerWorld level, BlockPos min, int size, ServerPlayerEntity stranger) throws ReflectiveOperationException {
        Class<?> storageClass = Class.forName("io.github.flemmli97.flan.claim.ClaimStorage");
        Class<?> claimClass = Class.forName("io.github.flemmli97.flan.claim.Claim");
        Class<?> permissionClass = Class.forName("io.github.flemmli97.flan.api.permission.ClaimPermission");
        Class<?> editModeClass = Class.forName("io.github.flemmli97.flan.player.EnumEditMode");
        Object storage = storageClass.getMethod("get", ServerWorld.class).invoke(null, level);
        Object claim = claimClass.getConstructor(int.class, int.class, int.class, int.class, int.class, UUID.class, ServerWorld.class)
                .newInstance(min.getX(), min.getX() + size - 1, min.getZ(), min.getZ() + size - 1, 0, null, level);
        claimClass.getMethod("setClaimID", UUID.class).invoke(claim, storageClass.getMethod("generateUUID").invoke(storage));
        Method addClaim = storageClass.getDeclaredMethod("addClaim", claimClass);
        addClaim.setAccessible(true);
        addClaim.invoke(storage, claim);

        BlockPos center = min.offset(size / 2, size / 2, size / 2);
        Object claimAtCenter = storageClass.getMethod("getClaimAt", BlockPos.class).invoke(storage, center);
        String flanAnswer;
        try {
            Object place = Class.forName("io.github.flemmli97.flan.api.permission.PermissionRegistry").getField("PLACE").get(null);
            flanAnswer = String.valueOf(claimClass.getMethod("canInteract", ServerPlayerEntity.class, permissionClass, BlockPos.class, boolean.class)
                    .invoke(claim, stranger, place, center, false));
        } catch (ReflectiveOperationException | RuntimeException e) {
            flanAnswer = e.toString();
        }
        LOGGER.info("claim probe benchmark | Flan admin claim at the box center: {}, Flan lets the stranger place there: {}",
                claimAtCenter == claim, flanAnswer);

        Object defaultMode = editModeClass.getField("DEFAULT").get(null);
        Method deleteClaim = storageClass.getMethod("deleteClaim", claimClass, boolean.class, editModeClass, ServerWorld.class);
        return () -> {
            try {
                deleteClaim.invoke(storage, claim, true, defaultMode, level);
            } catch (ReflectiveOperationException e) {
                LOGGER.error("claim probe benchmark: the Flan claim could not be deleted", e);
            }
        };
    }

    private static String median(long[] nanos) {
        long[] sorted = nanos.clone();
        Arrays.sort(sorted);
        return String.format("%.2f", sorted[sorted.length / 2] / 1e6);
    }
}
