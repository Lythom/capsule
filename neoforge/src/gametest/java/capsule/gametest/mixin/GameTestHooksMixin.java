package capsule.gametest.mixin;

import net.neoforged.neoforge.gametest.GameTestHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * NeoForge never enables GameTests in production: neither RegisterGameTestsEvent nor the GameTest ticker. On a
 * production GameTest server (-Dcapsule.gametest.production=true, see scripts/prod-gametest.sh) the test mod turns
 * both on. Undocumented NeoForge behavior, see BACKLOG.md.
 */
@Mixin(GameTestHooks.class)
public class GameTestHooksMixin {
    @Inject(method = "isGametestEnabled", at = @At("HEAD"), cancellable = true)
    private static void capsule$enableInProduction(CallbackInfoReturnable<Boolean> cir) {
        if (Boolean.getBoolean("capsule.gametest.production")) cir.setReturnValue(true);
    }
}
