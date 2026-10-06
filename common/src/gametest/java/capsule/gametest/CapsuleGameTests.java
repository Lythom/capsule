package capsule.gametest;

import capsule.CapsuleMod;
import capsule.platform.Services;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.StructureUtils;
import net.minecraft.gametest.framework.TestFunction;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Registers the @GameTest methods of the capsule test classes the same way on every loader: tests are named after their
 * method and use the templates of the capsule namespace.
 */
public class CapsuleGameTests {

    public static final List<Class<?>> TEST_CLASSES = List.of(
            BlueprintCostTests.class,
            BlueprintCraftingTests.class,
            BlueprintWhitelistTests.class,
            BundledContentTests.class,
            BundledTemplateContentTests.class,
            CaptureBaseTests.class,
            ClaimTests.class,
            ConfigTests.class,
            CoreMechanicsTests.class,
            DeployPositionTests.class,
            FurnaceExperienceTests.class,
            InfrastructureTests.class,
            ItemFrameTests.class,
            LootTests.class,
            PreviewQueryTests.class,
            RecallTests.class,
            RecipeTests.class,
            SchematicTests.class,
            ReloadTests.class,
            TemplateCopyTests.class,
            ThrowDeployTests.class,
            ThrowQueryTests.class,
            UndeployDelayTests.class
    );

    /**
     * Tests of optional mods, by mod id, run when the mod is loaded (-PmodCompat).
     */
    private static final Map<String, Class<?>> MOD_TEST_CLASSES = Map.of(
            "openpartiesandclaims", OpenPartiesAndClaimsTests.class,
            "flan", FlanTests.class
    );

    public static Stream<Class<?>> loadedModTestClasses() {
        return MOD_TEST_CLASSES.entrySet().stream()
                .filter(e -> Services.PLATFORM.isModLoaded(e.getKey()))
                .map(Map.Entry::getValue);
    }

    @GameTestGenerator
    public static Collection<TestFunction> generate() {
        return testFunctions(Stream.concat(TEST_CLASSES.stream(), loadedModTestClasses()));
    }

    public static List<TestFunction> testFunctions(Stream<Class<?>> testClasses) {
        Stream<Class<?>> proof = Boolean.getBoolean(FailureProofTests.PROPERTY) ? Stream.of(FailureProofTests.class) : Stream.empty();
        return Stream.concat(testClasses, proof)
                .flatMap(type -> Arrays.stream(type.getDeclaredMethods()))
                .filter(method -> method.isAnnotationPresent(GameTest.class) && Modifier.isStatic(method.getModifiers()))
                .sorted(Comparator.comparing(Method::getName))
                .map(CapsuleGameTests::testFunction)
                .toList();
    }

    private static TestFunction testFunction(Method method) {
        GameTest test = method.getAnnotation(GameTest.class);
        return new TestFunction(
                test.batch(),
                method.getName().toLowerCase(),
                CapsuleMod.MODID + ":" + test.template(),
                StructureUtils.getRotationForRotationSteps(test.rotationSteps()),
                test.timeoutTicks(),
                test.setupTicks(),
                test.required(),
                test.manualOnly(),
                test.requiredSuccesses(),
                test.attempts(),
                test.skyAccess(),
                helper -> invoke(method, helper)
        );
    }

    private static void invoke(Method method, GameTestHelper helper) {
        try {
            method.invoke(null, helper);
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof RuntimeException runtimeException) throw runtimeException;
            throw new RuntimeException(e.getCause());
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
