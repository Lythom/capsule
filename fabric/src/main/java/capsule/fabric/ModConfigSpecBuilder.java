package capsule.fabric;

import capsule.platform.ConfigSpecBuilder;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class ModConfigSpecBuilder implements ConfigSpecBuilder {
    private final ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

    public static ModConfigSpec build(Consumer<ConfigSpecBuilder> definition) {
        ModConfigSpecBuilder builder = new ModConfigSpecBuilder();
        definition.accept(builder);
        return builder.builder.build();
    }

    @Override
    public ConfigSpecBuilder comment(String comment) {
        builder.comment(comment);
        return this;
    }

    @Override
    public ConfigSpecBuilder worldRestart() {
        builder.worldRestart();
        return this;
    }

    @Override
    public ConfigSpecBuilder push(String path) {
        builder.push(path);
        return this;
    }

    @Override
    public ConfigSpecBuilder pop() {
        builder.pop();
        return this;
    }

    @Override
    public <T> Supplier<T> define(String path, T defaultValue) {
        return builder.define(path, defaultValue);
    }

    @Override
    public <T> Supplier<T> define(String path, T defaultValue, Predicate<Object> validator) {
        return builder.define(path, defaultValue, validator);
    }

    @Override
    public Supplier<Integer> defineInRange(String path, int defaultValue, int min, int max) {
        return builder.defineInRange(path, defaultValue, min, max);
    }

    @Override
    public Supplier<List<? extends String>> defineList(String path, List<String> defaultValue) {
        return builder.defineList(path, defaultValue, item -> item instanceof String);
    }
}
