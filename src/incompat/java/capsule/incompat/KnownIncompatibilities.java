package capsule.incompat;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Plays the scenario of each mod of the wiki page Known incompatibilities that is loaded, once a server started with
 * -Dcapsule.incompat=true (scripts/prod-smoke-forge.sh with INCOMPAT=1): each check of today's behavior logs a PASS or
 * FAIL line, then "capsule incompat: <n> checks, <m> failed". Never in the release jar.
 */
@Mod(KnownIncompatibilities.MODID)
public class KnownIncompatibilities {
    static final String MODID = "capsule_incompat";
    private static final Logger LOGGER = LogManager.getLogger("capsule incompat");

    private record Play(String mod, String name, Consumer<Scenario> scenario) {
    }

    private static final List<Play> PLAYS = List.of(
            new Play("tombstone", "Corail Tombstone graves", Scenarios::tombstoneGraves),
            new Play("refinedstorage", "Refined Storage, default config", Scenarios::refinedStorageExcluded),
            new Play("refinedstorage", "Refined Storage, not excluded", Scenarios::refinedStorageNotExcluded),
            new Play("mekanism", "Mekanism Digital Miner", Scenarios::mekanismDigitalMiner),
            new Play("mekanism", "Mekanism bin", Scenarios::mekanismBin),
            new Play("immersiveengineering", "Immersive Engineering wires", Scenarios::immersiveEngineeringWires),
            new Play("gtceu", "GregTech CEu machine", Scenarios::gregTechMachine),
            new Play("sfm", "Super Factory Manager", Scenarios::superFactoryManager),
            new Play("bloodmagic", "Blood Magic alchemy table", Scenarios::bloodMagicAlchemyTable)
    );

    private record Task(Scenario scenario, long tick, Runnable run) {
    }

    private static final List<Task> TASKS = new ArrayList<>();
    private static long tick;
    private static int checks, failed;
    private static boolean running;

    public KnownIncompatibilities() {
        if (Boolean.getBoolean("capsule.incompat")) MinecraftForge.EVENT_BUS.register(KnownIncompatibilities.class);
    }

    @SubscribeEvent
    public static void serverStarted(ServerStartedEvent event) {
        ServerLevel level = event.getServer().overworld();
        BlockPos spawn = level.getSharedSpawnPos();
        running = true;
        for (int i = 0; i < PLAYS.size(); i++) {
            Play play = PLAYS.get(i);
            if (!ModList.get().isLoaded(play.mod())) {
                LOGGER.info("SKIP | {}: {} is not loaded", play.name(), play.mod());
                continue;
            }
            Scenario scenario = new Scenario(play.name(), level, new BlockPos((spawn.getX() >> 4 << 4) + 32 * i, 200, (spawn.getZ() >> 4 << 4) + 64));
            later(scenario, 1, () -> play.scenario().accept(scenario));
        }
    }

    @SubscribeEvent
    public static void serverTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !running) return;
        tick++;
        for (Task task : List.copyOf(TASKS)) {
            if (task.tick() > tick) continue;
            TASKS.remove(task);
            try {
                task.run().run();
            } catch (RuntimeException | AssertionError e) {
                LOGGER.error("{} failed", task.scenario().name, e);
                check(task.scenario().name + ": the scenario runs", false, e);
            }
        }
        if (TASKS.isEmpty()) {
            running = false;
            LOGGER.info("capsule incompat: {} checks, {} failed", checks, failed);
        }
    }

    static void later(Scenario scenario, int ticks, Runnable task) {
        TASKS.add(new Task(scenario, tick + ticks, task));
    }

    static void check(String what, boolean passed, Object observed) {
        checks++;
        if (!passed) failed++;
        LOGGER.info("{} | {} | {}", passed ? "PASS" : "FAIL", what, observed);
    }
}
