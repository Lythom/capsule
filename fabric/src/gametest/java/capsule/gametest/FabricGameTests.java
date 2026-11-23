package capsule.gametest;

import capsule.gametest.CapsuleGameTests.CapsuleTest;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.gametest.framework.GameTestInstance;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class FabricGameTests {

    /**
     * The common tests, plus the Get Off My Lawn ones (a Fabric mod) when it is loaded, and the tests of the other
     * optional mods that are loaded.
     */
    public static final List<CapsuleTest> TESTS = tests();

    private static List<CapsuleTest> tests() {
        Stream<Class<?>> goml = FabricLoader.getInstance().isModLoaded("goml") ? Stream.of(GetOffMyLawnTests.class) : Stream.empty();
        return CapsuleGameTests.tests(Stream.of(CapsuleGameTests.TEST_CLASSES.stream(), goml, CapsuleGameTests.loadedModTestClasses()).flatMap(s -> s));
    }

    /**
     * Fabric API has no event for tests built at runtime: the mixin calls this while the datapack registries load,
     * before they are frozen. An environment per batch, then the test instances.
     */
    public static void registerInstances(Registry<TestEnvironmentDefinition<?>> environmentRegistry, Registry<GameTestInstance> instanceRegistry) {
        Map<Identifier, Holder<TestEnvironmentDefinition<?>>> environments = new HashMap<>();
        for (Identifier environment : CapsuleGameTests.environments(TESTS)) {
            environments.put(environment, Registry.registerForHolder(environmentRegistry, environment, new TestEnvironmentDefinition.AllOf(List.of())));
        }
        for (CapsuleTest test : TESTS) {
            Registry.register(instanceRegistry, test.id(), test.instance(environments.get(test.environment())));
        }
    }
}
