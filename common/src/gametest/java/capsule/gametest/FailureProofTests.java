package capsule.gametest;

import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Registered only with -Dcapsule.gametest.failOnPurpose=true, to prove that a test runner reports failures.
 */
public class FailureProofTests {
    static final String PROPERTY = "capsule.gametest.failOnPurpose";

    @CapsuleGameTest(template = "empty")
    public static void failsOnPurpose(GameTestHelper helper) {
        helper.fail("failed on purpose (-D" + PROPERTY + "=true)");
    }
}
