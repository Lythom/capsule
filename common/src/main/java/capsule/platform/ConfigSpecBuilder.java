package capsule.platform;

import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Defines the capsule config with the semantics of NeoForge's ModConfigSpec.Builder.
 */
public interface ConfigSpecBuilder {

    ConfigSpecBuilder comment(String comment);

    ConfigSpecBuilder worldRestart();

    ConfigSpecBuilder push(String path);

    ConfigSpecBuilder pop();

    <T> Supplier<T> define(String path, T defaultValue);

    <T> Supplier<T> define(String path, T defaultValue, Predicate<Object> validator);

    Supplier<Integer> defineInRange(String path, int defaultValue, int min, int max);

    Supplier<List<? extends String>> defineList(String path, List<String> defaultValue);
}
