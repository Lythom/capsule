package capsule.fabric.mixin;

import capsule.items.CapsuleItems;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundUpdateRecipesPacket;
import net.minecraft.world.item.crafting.RecipeManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Where NeoForge fires RecipesUpdatedEvent: the creative tab and the recipe viewers list capsules from the recipes, so
 * the capsule lists are filled as soon as the recipes are replaced, before JEI reads them at the end of the method.
 */
@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin {
    @Shadow
    @Final
    private RecipeManager recipeManager;

    @Inject(method = "handleUpdateRecipes", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/crafting/RecipeManager;replaceRecipes(Ljava/lang/Iterable;)V", shift = At.Shift.AFTER))
    private void capsule$onRecipesUpdated(ClientboundUpdateRecipesPacket packet, CallbackInfo ci) {
        CapsuleItems.registerRecipesClient(recipeManager);
    }
}
