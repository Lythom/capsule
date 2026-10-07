package capsule.clientsmoke;

import io.github.flemmli97.flan.claim.Claim;
import io.github.flemmli97.flan.claim.ClaimStorage;
import io.github.flemmli97.flan.config.ConfigHandler;
import io.github.flemmli97.flan.player.PlayerClaimData;
import io.github.flemmli97.flan.player.display.EnumDisplayType;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

/**
 * Flan calls of the showcase, loaded only when Flan is.
 */
class ShowcaseFlan {
    /**
     * A claim of owner from one corner to the other, columns from the lowest corner up.
     */
    static Object claim(ServerPlayer player, BlockPos from, BlockPos to, UUID owner) {
        ServerLevel level = player.serverLevel();
        ClaimStorage storage = ClaimStorage.get(level);
        Claim claim = storage.createAdminClaim(from, to, level, false);
        storage.transferOwner(claim, owner);
        return claim;
    }

    /**
     * Shows the borders of the claim to the player for a minute.
     */
    static void display(ServerPlayer player, Object claim) {
        ConfigHandler.CONFIG.claimDisplayTime = 20 * 60;
        PlayerClaimData.get(player).addDisplayClaim((Claim) claim, EnumDisplayType.MAIN, player.getBlockY());
    }
}
