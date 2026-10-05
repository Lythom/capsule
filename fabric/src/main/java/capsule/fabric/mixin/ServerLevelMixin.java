package capsule.fabric.mixin;

import capsule.StructureSaver;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Where NeoForge fires EntityJoinLevelEvent: drops of the blocks removed by a capture never spawn.
 */
@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin {

    @Inject(method = "addFreshEntity", at = @At("HEAD"), cancellable = true)
    private void capsule$preventCaptureDrops(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (StructureSaver.isPreventedDrop(entity)) {
            cir.setReturnValue(false);
        }
    }
}
