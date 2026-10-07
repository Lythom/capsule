package capsule.gametest;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.TestFunction;

import java.util.Collection;
import java.util.stream.Stream;

public class FabricGameTests {

    /**
     * The common tests, plus the Get Off My Lawn ones (a Fabric mod) when it is loaded, and the tests of the other
     * optional mods that are loaded.
     */
    @GameTestGenerator
    public static Collection<TestFunction> generate() {
        Stream<Class<?>> goml = FabricLoader.getInstance().isModLoaded("goml") ? Stream.of(GetOffMyLawnTests.class) : Stream.empty();
        return CapsuleGameTests.testFunctions(Stream.of(CapsuleGameTests.TEST_CLASSES.stream(), goml, CapsuleGameTests.loadedModTestClasses()).flatMap(s -> s));
    }
}
