package capsule.gametest;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A capsule GameTest: a static method taking a GameTestHelper. Since Minecraft 1.21.5 tests are registry entries, and
 * the vanilla annotation is gone: {@link CapsuleGameTests} turns these methods into test functions and test instances
 * the same way on every loader. The attributes keep the names of the vanilla annotation of 1.21.1.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface CapsuleGameTest {
    /**
     * Structure of the capsule namespace the test runs in.
     */
    String template();

    /**
     * Tests of the same batch run together, apart from the other batches: each batch is a test environment.
     */
    String batch() default "";

    int timeoutTicks() default 100;

    int setupTicks() default 0;

    boolean required() default true;

    int attempts() default 1;

    int requiredSuccesses() default 1;

    boolean skyAccess() default false;

    boolean manualOnly() default false;

    int rotationSteps() default 0;
}
