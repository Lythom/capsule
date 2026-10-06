package capsule.gametest;

import com.mojang.authlib.GameProfile;
import eu.pb4.common.protection.api.CommonProtection;
import eu.pb4.common.protection.api.ProtectionProvider;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nullable;

public class FabricGameTestSetup implements ModInitializer {

    @Override
    public void onInitialize() {
        ServerLifecycleEvents.SERVER_STARTING.register(GameTestProfiles::provideProfileCache);
        CommonProtection.register(ResourceLocation.fromNamespaceAndPath("capsule-gametest", "test_probe"), new ProtectionProvider() {
            @Override
            public boolean isProtected(Level level, BlockPos pos) {
                return false;
            }

            @Override
            public boolean isAreaProtected(Level level, AABB area) {
                return false;
            }

            @Override
            public boolean canPlaceBlock(Level level, BlockPos pos, GameProfile profile, @Nullable Player player) {
                return TestProbe.canPlace(pos);
            }
        });
    }
}
