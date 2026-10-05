package capsule.fabric.mixin;

import capsule.items.CapsuleItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Where NeoForge fires PlayerInteractEvent.LeftClickEmpty: left click in the air rotates or reloads capsules.
 */
@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
    @Shadow
    public LocalPlayer player;

    @Inject(method = "startAttack", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;resetAttackStrengthTicker()V", shift = At.Shift.AFTER))
    private void capsule$onLeftClickEmpty(CallbackInfoReturnable<Boolean> cir) {
        CapsuleItem.onLeftClickEmpty(player);
    }
}
