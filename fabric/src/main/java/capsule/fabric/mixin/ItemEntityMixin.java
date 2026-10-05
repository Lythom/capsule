package capsule.fabric.mixin;

import capsule.items.CapsuleItem;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Where NeoForge calls IItemExtension#onEntityItemUpdate: thrown capsules deploy on collision.
 */
@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin {

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void capsule$onEntityItemUpdate(CallbackInfo ci) {
        ItemEntity self = (ItemEntity) (Object) this;
        ItemStack stack = self.getItem();
        if (stack.getItem() instanceof CapsuleItem capsule && capsule.onEntityItemUpdate(stack, self)) {
            ci.cancel();
        }
    }
}
