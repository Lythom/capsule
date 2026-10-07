package capsule.gametest;

import capsule.platform.Services;
import capsule.plugins.claims.Claims;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.function.Supplier;

/**
 * Cost of the generic claim probe (a placement event on NeoForge, Common Protection API on Fabric) asked for every
 * block or once per chunk column of a capture of 3, 11, 31 and 255, by a stranger, outside claims and, when Flan or
 * Open Parties and Claims is loaded (-PmodCompat), inside a claim of each; and of the adapters (Claims.denied, which
 * asks them for the whole box). Registered only with -Dcapsule.gametest.claimBenchmark=true (-PclaimBenchmark); the
 * median times are logged as "claim probe benchmark".
 */
public class ClaimProbeBenchmark {
    static final String PROPERTY = "capsule.gametest.claimBenchmark";
    private static final Logger LOGGER = LogManager.getLogger(ClaimProbeBenchmark.class);
    private static final int[] SIZES = {3, 11, 31, 255};

    @GameTest(template = "empty", batch = "claimbenchmark")
    public static void claimProbeCost(GameTestHelper helper) {
        ServerPlayer owner = CapsuleTestUtils.survivalPlayer(helper, new BlockPos(0, 1, 0));
        ServerPlayer stranger = CapsuleTestUtils.survivalPlayer(helper, new BlockPos(4, 1, 4));
        BlockPos min = helper.absolutePos(new BlockPos(0, 1, 0));
        BoundingBox largest = box(min, SIZES[SIZES.length - 1]);
        // loads the chunks and the probe code before measuring
        perColumn(helper.getLevel(), largest, stranger);
        perBlock(helper.getLevel(), box(min, 31), stranger);
        measure(helper, "no claim", min, stranger);

        try {
            if (Services.PLATFORM.isModLoaded("flan")) {
                measureInside(helper, "Flan claim", () -> FlanTests.claim(helper, largest, owner), min, stranger);
            }
            if (Services.PLATFORM.isModLoaded("openpartiesandclaims")) {
                measureInside(helper, "Open Parties and Claims claim", () -> OpenPartiesAndClaimsTests.claim(helper, largest, owner), min, stranger);
            }
        } finally {
            CapsuleTestUtils.removePlayer(owner);
            CapsuleTestUtils.removePlayer(stranger);
        }
        helper.succeed();
    }

    private static void measureInside(GameTestHelper helper, String scenario, Supplier<Runnable> claim, BlockPos min, ServerPlayer player) {
        Runnable unclaim = claim.get();
        try {
            measure(helper, scenario, min, player);
        } finally {
            unclaim.run();
        }
    }

    private static void measure(GameTestHelper helper, String scenario, BlockPos min, ServerPlayer player) {
        String loader = Services.PLATFORM.isModLoaded("neoforge") ? "NeoForge" : "Fabric";
        for (int size : SIZES) {
            BoundingBox box = box(min, size);
            int runs = size < 255 ? 7 : 3;
            long[] blockTimes = new long[runs];
            long[] columnTimes = new long[runs];
            long[] adapterTimes = new long[runs];
            int deniedBlocks = 0;
            int deniedColumns = 0;
            for (int run = 0; run < runs; run++) {
                long start = System.nanoTime();
                deniedBlocks = perBlock(helper.getLevel(), box, player);
                blockTimes[run] = System.nanoTime() - start;
                start = System.nanoTime();
                deniedColumns = perColumn(helper.getLevel(), box, player);
                columnTimes[run] = System.nanoTime() - start;
                start = System.nanoTime();
                Claims.denied(helper.getLevel(), box, player);
                adapterTimes[run] = System.nanoTime() - start;
            }
            int columns = ((box.maxX() >> 4) - (box.minX() >> 4) + 1) * ((box.maxZ() >> 4) - (box.minZ() >> 4) + 1);
            LOGGER.info("claim probe benchmark | {} | {} | size {} | per block: {} probes, {} denied, {} ms | per chunk column: {} probes, {} denied, {} ms | adapters: {} ms | median of {}",
                    loader, scenario, size, (long) size * size * size, deniedBlocks, median(blockTimes), columns, deniedColumns, median(columnTimes), median(adapterTimes), runs);
        }
    }

    private static int perBlock(ServerLevel level, BoundingBox box, ServerPlayer player) {
        int denied = 0;
        for (BlockPos pos : BlockPos.betweenClosed(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ())) {
            if (!Services.PLATFORM.canPlaceBlock(level, pos, player)) denied++;
        }
        return denied;
    }

    private static int perColumn(ServerLevel level, BoundingBox box, ServerPlayer player) {
        int denied = 0;
        for (int chunkX = box.minX() >> 4; chunkX <= box.maxX() >> 4; chunkX++) {
            for (int chunkZ = box.minZ() >> 4; chunkZ <= box.maxZ() >> 4; chunkZ++) {
                int x = Math.clamp((chunkX << 4) + 8, box.minX(), box.maxX());
                int z = Math.clamp((chunkZ << 4) + 8, box.minZ(), box.maxZ());
                if (!Services.PLATFORM.canPlaceBlock(level, new BlockPos(x, box.getCenter().getY(), z), player)) denied++;
            }
        }
        return denied;
    }

    private static BoundingBox box(BlockPos min, int size) {
        return BoundingBox.fromCorners(min, min.offset(size - 1, size - 1, size - 1));
    }

    private static String median(long[] nanos) {
        long[] sorted = nanos.clone();
        Arrays.sort(sorted);
        return String.format("%.2f", sorted[sorted.length / 2] / 1e6);
    }
}
