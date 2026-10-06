package capsule.client;

import capsule.platform.ConfigSpecBuilder;

import java.util.function.Supplier;

public class ClientConfig {

    public static boolean captureAnimation = true;

    private static Supplier<Boolean> captureAnimationCfg;

    /**
     * Defines the client config, saved by the loaders in config/capsule-client.toml.
     */
    public static void define(ConfigSpecBuilder builder) {
        captureAnimationCfg = builder.comment("Show the captured blocks being sucked into the capsule.\nDefault value: true.")
                .define("captureAnimation", true);
    }

    /**
     * Reads the config values, called by the loaders when the config is loaded or changed.
     */
    public static void bakeConfig() {
        captureAnimation = captureAnimationCfg.get();
    }
}
