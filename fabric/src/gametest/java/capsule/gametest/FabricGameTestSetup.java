package capsule.gametest;

import eu.pb4.common.protection.api.CommonProtection;
import eu.pb4.common.protection.api.ProtectionProvider;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.players.NameAndId;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nullable;

public class FabricGameTestSetup implements ModInitializer {

    @Override
    public void onInitialize() {
        // the test instances are registered with the datapack registries, see RegistryDataLoaderMixin
        for (CapsuleGameTests.CapsuleTest test : FabricGameTests.TESTS) {
            Registry.register(BuiltInRegistries.TEST_FUNCTION, test.id(), test.function());
        }
        ServerLifecycleEvents.SERVER_STARTING.register(GameTestProfiles::provideProfileCache);
        CommonProtection.register(Identifier.fromNamespaceAndPath("capsule-gametest", "test_probe"), new ProtectionProvider() {
            @Override
            public boolean isProtected(Level level, BlockPos pos) {
                return false;
            }

            @Override
            public boolean isAreaProtected(Level level, AABB area) {
                return false;
            }

            @Override
            public boolean canPlaceBlock(Level level, BlockPos pos, NameAndId profile, @Nullable Player player) {
                return TestProbe.canPlace(pos);
            }
        });
    }
}
