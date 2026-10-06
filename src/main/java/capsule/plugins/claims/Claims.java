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
    /**
     * Asks the claims for captures and deploys without a player (dispensers, capture bases placed before this version).
     */
    private static final GameProfile NOBODY = new GameProfile(UUID.fromString("9c0b9b7b-b356-41c0-93b2-4bb6afe1586c"), "[Capsule]");

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
     * The player the claims are asked for: player, or without a player an anonymous fake player that no claim lists as
     * a member, so that nobody may change a claimed position.
     */
    public static ServerPlayerEntity actor(ServerWorld level, @Nullable ServerPlayerEntity player) {
        return player != null ? player : FakePlayerFactory.get(level, NOBODY);
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
