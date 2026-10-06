package capsule.gametest;

import com.mojang.authlib.GameProfileRepository;
import com.mojang.authlib.ProfileLookupCallback;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.Services;
import net.minecraft.server.players.GameProfileCache;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.util.Arrays;

/**
 * The GameTest server has no profile cache, which real servers have and claim mods (Open Parties and Claims, Flan)
 * use: the loaders give it an offline one before the server starts.
 */
public class GameTestProfiles {

    public static void provideProfileCache(MinecraftServer server) {
        if (server.getProfileCache() != null) return;
        try {
            File file = File.createTempFile("capsule-gametest-usercache", ".json");
            file.deleteOnExit();
            Files.writeString(file.toPath(), "[]");
            GameProfileRepository offline = new GameProfileRepository() {
                @Override
                public void findProfilesByNames(String[] names, ProfileLookupCallback callback) {
                    Arrays.stream(names).forEach(name -> callback.onProfileLookupFailed(name, new IllegalStateException("offline")));
                }
            };
            Field services = Arrays.stream(MinecraftServer.class.getDeclaredFields())
                    .filter(f -> f.getType() == Services.class)
                    .findFirst().orElseThrow();
            services.setAccessible(true);
            Services current = (Services) services.get(server);
            services.set(server, new Services(current.sessionService(), current.servicesKeySet(), offline, new GameProfileCache(offline, file)));
        } catch (IOException | ReflectiveOperationException e) {
            throw new IllegalStateException("could not give the GameTest server a profile cache", e);
        }
    }
}
