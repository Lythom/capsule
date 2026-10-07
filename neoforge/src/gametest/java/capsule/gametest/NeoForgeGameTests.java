package capsule.gametest;

import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.TestFunction;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Stream;

@EventBusSubscriber(modid = NeoForgeGameTests.MODID, bus = EventBusSubscriber.Bus.MOD)
public class NeoForgeGameTests {
    /**
     * The GameTests and the client smoke test are a mod of their own, never in the release jar.
     */
    public static final String MODID = "capsule_gametest";

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        event.register(NeoForgeGameTests.class);
    }

    /**
     * The common tests, plus the SecurityCraft ones (SecurityCraft is a GameTest dependency of NeoForge only), and the
     * tests of the optional mods that are loaded.
     */
    @GameTestGenerator
    public static Collection<TestFunction> generate() {
        Stream<Class<?>> optional = Map.of(
                        "waystones", WaystonesTests.class, "sophisticatedstorage", SophisticatedStorageTests.class, "worldedit", WorldEditTests.class,
                        // the known incompatibilities (-Pincompat)
                        "tombstone", TombstoneTests.class, "refinedstorage", RefinedStorageTests.class, "mekanism", MekanismTests.class,
                        "immersiveengineering", ImmersiveEngineeringTests.class, "sfm", SuperFactoryManagerTests.class).entrySet().stream()
                .filter(e -> ModList.get().isLoaded(e.getKey()))
                .map(Map.Entry::getValue);
        return CapsuleGameTests.testFunctions(Stream.of(CapsuleGameTests.TEST_CLASSES.stream(), Stream.of(SecurityCraftTests.class), optional, CapsuleGameTests.loadedModTestClasses()).flatMap(s -> s));
    }
}
