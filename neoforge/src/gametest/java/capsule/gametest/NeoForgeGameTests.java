package capsule.gametest;

import capsule.gametest.CapsuleGameTests.CapsuleTest;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Registers the capsule GameTests: their functions in the test function registry, then, when NeoForge loads the
 * datapack registries with GameTests enabled, an environment per batch and the test instances.
 */
@Mod(NeoForgeGameTests.MODID)
public class NeoForgeGameTests {
    /**
     * The GameTests and the client smoke test are a mod of their own, never in the release jar.
     */
    public static final String MODID = "capsule_gametest";

    private final List<CapsuleTest> tests = tests();

    public NeoForgeGameTests(IEventBus modBus) {
        // the tests are in the capsule namespace, as on Fabric, so the instances and functions are registered by id
        modBus.addListener((RegisterEvent event) -> event.register(Registries.TEST_FUNCTION,
                registry -> tests.forEach(test -> registry.register(test.id(), test.function()))));
        modBus.addListener(this::register);
    }

    private void register(RegisterGameTestsEvent event) {
        Map<Identifier, Holder<TestEnvironmentDefinition<?>>> environments = new HashMap<>();
        for (Identifier environment : CapsuleGameTests.environments(tests)) {
            environments.put(environment, event.registerEnvironment(environment));
        }
        for (CapsuleTest test : tests) {
            event.registerTest(test.id(), test.instance(environments.get(test.environment())));
        }
    }

    /**
     * The common tests, plus the SecurityCraft ones (SecurityCraft is a GameTest dependency of NeoForge only), and the
     * tests of the optional mods that are loaded.
     */
    private static List<CapsuleTest> tests() {
        Stream<Class<?>> optional = Map.of(
                        "waystones", WaystonesTests.class, "sophisticatedstorage", SophisticatedStorageTests.class, "worldedit", WorldEditTests.class,
                        // the known incompatibilities (-Pincompat; Tombstone has no 26.3 build, so this one is never loaded
                        // for now). Refined Storage, Mekanism, Immersive Engineering and Super Factory Manager have no
                        // 26.2 or 26.3 build: their tests are left out of the build (build.gradle)
                        "tombstone", TombstoneTests.class).entrySet().stream()
                .filter(e -> ModList.get().isLoaded(e.getKey()))
                .map(Map.Entry::getValue);
        return CapsuleGameTests.tests(Stream.of(CapsuleGameTests.TEST_CLASSES.stream(), Stream.of(SecurityCraftTests.class), optional, CapsuleGameTests.loadedModTestClasses()).flatMap(s -> s));
    }
}
