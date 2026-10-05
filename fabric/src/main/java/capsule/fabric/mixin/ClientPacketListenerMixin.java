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
 * Where NeoForge fires RecipesUpdatedEvent: the creative tab and JEI list capsules from the recipes.
 */
@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin {
    @Shadow
    @Final
    private RecipeManager recipeManager;

    @Inject(method = "handleUpdateRecipes", at = @At("TAIL"))
    private void capsule$onRecipesUpdated(ClientboundUpdateRecipesPacket packet, CallbackInfo ci) {
        CapsuleItems.registerRecipesClient(recipeManager);
    }
}
