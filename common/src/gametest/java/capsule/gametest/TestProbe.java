package capsule.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A claim mod without adapter, as the generic claim probe sees it: the loader's protection hook (placement event on
 * NeoForge, Common Protection API on Fabric) counts its queries in area and refuses the protected positions, until
 * closed.
 */
public class TestProbe implements AutoCloseable {
    private static final List<TestProbe> PROBES = new CopyOnWriteArrayList<>();
    final BoundingBox area;
    final Set<BlockPos> protectedPositions;
    final AtomicInteger queries = new AtomicInteger();

    TestProbe(BoundingBox area, Set<BlockPos> protectedPositions) {
        this.area = area;
        this.protectedPositions = protectedPositions;
        PROBES.add(this);
    }

    @Override
    public void close() {
        PROBES.remove(this);
    }

    public static boolean canPlace(BlockPos pos) {
        boolean allowed = true;
        for (TestProbe probe : PROBES) {
            if (!probe.area.isInside(pos)) continue;
            probe.queries.incrementAndGet();
            if (probe.protectedPositions.contains(pos)) allowed = false;
        }
        return allowed;
    }
}
