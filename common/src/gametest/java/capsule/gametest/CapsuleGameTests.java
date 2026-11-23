package capsule.gametest;

import capsule.CapsuleMod;
import capsule.platform.Services;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.StructureUtils;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Turns the @CapsuleGameTest methods of the capsule test classes into tests the same way on every loader: tests are
 * named after their method, use the templates of the capsule namespace, and each batch is a test environment.
 */
public class CapsuleGameTests {

    public static final List<Class<?>> TEST_CLASSES = List.of(
            BlueprintCostTests.class,
            BlueprintCraftingTests.class,
            BlueprintWhitelistTests.class,
            BundledConfigTests.class,
            BundledContentTests.class,
            BundledTemplateContentTests.class,
            CaptureBaseTests.class,
            ClaimTests.class,
            CommandTests.class,
            ConfigTests.class,
            CoreMechanicsTests.class,
            DeployPositionTests.class,
            FurnaceExperienceTests.class,
            InfrastructureTests.class,
            ItemFrameTests.class,
            LinkedTemplateCopyTests.class,
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

    /**
     * The environment of the tests without a batch.
     */
    public static final String DEFAULT_BATCH = "default";

    /**
     * A test of a capsule test class, ready to be registered: its function goes to the test function registry, its
     * instance to the test instance registry, in the environment named after its batch.
     */
    public record CapsuleTest(Identifier id, Consumer<GameTestHelper> function, String batch, CapsuleGameTest meta) {
        public Identifier environment() {
            return environmentId(batch);
        }

        public FunctionGameTestInstance instance(Holder<TestEnvironmentDefinition<?>> environment) {
            return new FunctionGameTestInstance(
                    ResourceKey.create(Registries.TEST_FUNCTION, id),
                    new TestData<>(
                            environment,
                            Identifier.fromNamespaceAndPath(CapsuleMod.MODID, meta.template()),
                            meta.timeoutTicks(),
                            meta.setupTicks(),
                            meta.required(),
                            StructureUtils.getRotationForRotationSteps(meta.rotationSteps()),
                            meta.manualOnly(),
                            meta.attempts(),
                            meta.requiredSuccesses(),
                            meta.skyAccess(),
                            1
                    ));
        }
    }

    /**
     * Identifiers are lower case: batch "prefabPattern" is environment capsule:prefab_pattern.
     */
    public static Identifier environmentId(String batch) {
        return Identifier.fromNamespaceAndPath(CapsuleMod.MODID, batch.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase(Locale.ROOT));
    }

    /**
     * The tests of the given classes, plus the on-demand ones whose system property is set.
     */
    public static List<CapsuleTest> tests(Stream<Class<?>> testClasses) {
        Stream<Class<?>> onDemand = Map.<String, Class<?>>of(FailureProofTests.PROPERTY, FailureProofTests.class, ClaimProbeBenchmark.PROPERTY, ClaimProbeBenchmark.class)
                .entrySet().stream().filter(e -> Boolean.getBoolean(e.getKey())).map(Map.Entry::getValue);
        return Stream.concat(testClasses, onDemand)
                .flatMap(type -> Arrays.stream(type.getDeclaredMethods()))
                .filter(method -> method.isAnnotationPresent(CapsuleGameTest.class) && Modifier.isStatic(method.getModifiers()))
                .sorted(Comparator.comparing(Method::getName))
                .map(CapsuleGameTests::test)
                .toList();
    }

    /**
     * The environments of the given tests, once each.
     */
    public static Set<Identifier> environments(List<CapsuleTest> tests) {
        return tests.stream().map(CapsuleTest::environment).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static CapsuleTest test(Method method) {
        CapsuleGameTest test = method.getAnnotation(CapsuleGameTest.class);
        return new CapsuleTest(
                Identifier.fromNamespaceAndPath(CapsuleMod.MODID, method.getName().toLowerCase(Locale.ROOT)),
                helper -> invoke(method, helper),
                test.batch().isEmpty() ? DEFAULT_BATCH : test.batch(),
                test
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
