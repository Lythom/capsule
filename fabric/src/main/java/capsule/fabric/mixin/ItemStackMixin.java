package capsule.fabric.mixin;

import capsule.items.CapsuleItem;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Where NeoForge fires EnchantmentLevelSetEvent: enchanting tables offer nothing for one-use capsules.
 */
@Mixin(ItemStack.class)
public abstract class ItemStackMixin {

    @Inject(method = "isEnchantable", at = @At("HEAD"), cancellable = true)
    private void capsule$isEnchantable(CallbackInfoReturnable<Boolean> cir) {
        if (CapsuleItem.refusesEnchantingTable((ItemStack) (Object) this)) {
            cir.setReturnValue(false);
        }
    }
}
