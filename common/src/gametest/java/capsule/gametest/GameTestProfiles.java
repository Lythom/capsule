package capsule.gametest;

import com.mojang.authlib.GameProfileRepository;
import com.mojang.authlib.ProfileLookupCallback;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.Services;
import net.minecraft.server.players.CachedUserNameToIdResolver;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Optional;

/**
 * The GameTest server has a mock name to id cache, where real servers have a cached one that claim mods (Open Parties
 * and Claims, Flan) use, Open Parties and Claims only if it is the vanilla one: the loaders give the GameTest server an
 * offline vanilla cache before the server starts.
 */
public class GameTestProfiles {

    public static void provideProfileCache(MinecraftServer server) {
        if (server.services().nameToIdCache() instanceof CachedUserNameToIdResolver) return;
        try {
            File file = File.createTempFile("capsule-gametest-usercache", ".json");
            file.deleteOnExit();
            Files.writeString(file.toPath(), "[]");
            GameProfileRepository offline = new GameProfileRepository() {
                @Override
                public void findProfilesByNames(String[] names, ProfileLookupCallback callback) {
                    Arrays.stream(names).forEach(name -> callback.onProfileLookupFailed(name, new IllegalStateException("offline")));
                }

                @Override
                public Optional<com.mojang.authlib.services.response.NameAndId> findProfileByName(String name) {
                    return Optional.empty();
                }
            };
            Field services = Arrays.stream(MinecraftServer.class.getDeclaredFields())
                    .filter(f -> f.getType() == Services.class)
                    .findFirst().orElseThrow();
            services.setAccessible(true);
            Services current = (Services) services.get(server);
            services.set(server, new Services(current.sessionService(), current.servicesKeySet(), offline, new CachedUserNameToIdResolver(offline, file), current.profileResolver()));
        } catch (IOException | ReflectiveOperationException e) {
            throw new IllegalStateException("could not give the GameTest server a profile cache", e);
        }
    }
}
