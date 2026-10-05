package capsule.platform;

import java.util.ServiceLoader;

/**
 * Loader specific implementations, found with {@link ServiceLoader} in the META-INF/services files of each loader jar.
 */
public final class Services {
    public static final Platform PLATFORM = load(Platform.class);
    public static final Network NETWORK = load(Network.class);

    private Services() {
    }

    /**
     * Client implementations are loaded on first use, so that dedicated servers never load them.
     */
    public static ClientPlatform client() {
        return Client.PLATFORM;
    }

    public static <T> T load(Class<T> type) {
        return ServiceLoader.load(type, Services.class.getClassLoader())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No implementation of " + type.getName()));
    }

    private static final class Client {
        static final ClientPlatform PLATFORM = load(ClientPlatform.class);
    }
}
