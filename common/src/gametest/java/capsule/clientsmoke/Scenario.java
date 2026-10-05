package capsule.clientsmoke;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * Steps run on client ticks, one at a time: a step runs every tick until it returns true or its timeout expires. A
 * timeout or an exception records a failure and skips to the final steps, which always run.
 */
class Scenario {
    private static final Logger LOGGER = LogManager.getLogger();

    private record Step(String name, int timeout, BooleanSupplier action) {
    }

    private final Deque<Step> steps = new ArrayDeque<>();
    private final Deque<Step> finalSteps = new ArrayDeque<>();
    final List<String> results = new ArrayList<>();
    private Step current;
    private boolean inFinalSteps;
    private int ticks;
    private boolean failed;

    Scenario await(String name, int timeout, BooleanSupplier condition) {
        steps.add(new Step(name, timeout, condition));
        return this;
    }

    Scenario run(String name, Runnable action) {
        return await(name, 1, () -> {
            action.run();
            return true;
        });
    }

    Scenario sleep(int ticks) {
        int[] left = {ticks};
        return await("sleep " + ticks, ticks + 1, () -> --left[0] <= 0);
    }

    /**
     * Runs an action whose completion is asynchronous, such as a task on the server thread.
     */
    Scenario async(String name, int timeout, Supplier<CompletableFuture<?>> action) {
        CompletableFuture<?>[] future = {null};
        return await(name, timeout, () -> {
            if (future[0] == null) future[0] = action.get();
            if (!future[0].isDone()) return false;
            future[0].join();
            return true;
        });
    }

    Scenario finallyRun(String name, Runnable action) {
        finalSteps.add(new Step(name, 1, () -> {
            action.run();
            return true;
        }));
        return this;
    }

    Scenario finallyAwait(String name, int timeout, BooleanSupplier condition) {
        finalSteps.add(new Step(name, timeout, condition));
        return this;
    }

    void check(String name, boolean ok, String detail) {
        results.add((ok ? "PASS " : "FAIL ") + name + (detail.isEmpty() ? "" : ": " + detail));
        if (!ok) {
            failed = true;
            LOGGER.error("Client smoke check failed: {} {}", name, detail);
        }
    }

    boolean failed() {
        return failed;
    }

    boolean done() {
        return current == null && steps.isEmpty() && finalSteps.isEmpty();
    }

    void tick() {
        if (current == null) {
            inFinalSteps = steps.isEmpty();
            current = inFinalSteps ? finalSteps.poll() : steps.poll();
            ticks = 0;
            if (current == null) return;
            LOGGER.info("Client smoke step: {}", current.name);
        }
        boolean finished;
        try {
            finished = current.action.getAsBoolean();
        } catch (Throwable e) {
            LOGGER.error("Client smoke step {} threw", current.name, e);
            check(current.name, false, e.toString());
            abort();
            return;
        }
        if (finished) {
            current = null;
        } else if (++ticks >= current.timeout) {
            check(current.name, false, "timed out after " + current.timeout + " ticks");
            abort();
        }
    }

    private void abort() {
        current = null;
        if (!inFinalSteps) steps.clear();
    }
}
