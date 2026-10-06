package capsule.plugins.claims;

import com.mojang.authlib.GameProfile;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.server.management.PlayerProfileCache;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.util.FakePlayerFactory;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * The player claim mods check a capture or deploy as. Each position is then asked to them with a block placement event.
 */
public final class Claims {

    private Claims() {
    }

    /**
     * The player a capture or deploy is checked as: the connected player with this id, or a fake player with their
     * profile when they are offline.
     */
    @Nullable
    public static ServerPlayerEntity player(ServerWorld level, @Nullable UUID id) {
        if (id == null) return null;
        PlayerEntity player = level.getPlayerByUUID(id);
        if (player instanceof ServerPlayerEntity) return (ServerPlayerEntity) player;
        ServerPlayerEntity connected = level.getServer().getPlayerList().getPlayer(id);
        return connected != null ? connected : fakePlayer(level, id);
    }

    /**
     * A fake player with the profile of id, which gets no chat feedback.
     */
    @Nullable
    public static ServerPlayerEntity fakePlayer(ServerWorld level, @Nullable UUID id) {
        if (id == null) return null;
        PlayerProfileCache profiles = level.getServer().getProfileCache();
        GameProfile profile = profiles == null ? null : profiles.get(id);
        return FakePlayerFactory.get(level, profile != null ? profile : new GameProfile(id, "[Capsule]"));
    }
}
