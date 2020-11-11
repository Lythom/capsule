package capsule.config;

import com.electronwill.nightconfig.core.AbstractCommentedConfig;
import com.electronwill.nightconfig.core.ConfigFormat;
import com.electronwill.nightconfig.core.UnmodifiableConfig;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;

/**
 * Default concrete implementation of CommentedConfig. The values are stored in a map, generally a
 * HashMap, or a ConcurrentHashMap if the config is concurrent.
 *
 * @author TheElectronWill
 */
public final class SimpleCommentedConfig extends AbstractCommentedConfig {
    private final ConfigFormat<?> configFormat;

    /**
     * Creates a SimpleCommentedConfig with the specified format.
     *
     * @param configFormat the config's format
     */
    public SimpleCommentedConfig(ConfigFormat<?> configFormat) {
        super(new HashMap<>());
        this.configFormat = configFormat;
    }

    /**
     * Creates a SimpleCommentedConfig with the specified backing map supplier and format.
     *
     * @param mapCreator the supplier for backing maps
     * @param configFormat the config's format
     */
    SimpleCommentedConfig(Supplier<Map<String, Object>> mapCreator, ConfigFormat<?> configFormat) {
        super(mapCreator);
        this.configFormat = configFormat;
    }

    /**
     * Creates a SimpleCommentedConfig by copying a config, with the specified backing map creator and format.
     *
     * @param toCopy       the config to copy
     * @param mapCreator   the supplier for backing maps
     * @param configFormat the config's format
     */
    public SimpleCommentedConfig(UnmodifiableConfig toCopy, Supplier<Map<String, Object>> mapCreator,
                                 ConfigFormat<?> configFormat) {
        super(toCopy, mapCreator);
        this.configFormat = configFormat;
    }

    @Override
    public ConfigFormat<?> configFormat() {
        return configFormat;
    }

    @Override
    public SimpleCommentedConfig createSubConfig() {
        return new SimpleCommentedConfig(mapCreator, configFormat);
    }

    @Override
    public AbstractCommentedConfig clone() {
        return new SimpleCommentedConfig(this, mapCreator, configFormat);
    }
}