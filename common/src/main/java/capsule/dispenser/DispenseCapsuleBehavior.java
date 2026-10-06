package capsule.dispenser;

import capsule.blocks.BlockEntityCapture;
import capsule.helpers.Capsule;
import capsule.helpers.Spacial;
import capsule.items.CapsuleItem;
import capsule.plugins.claims.Claims;
import net.minecraft.core.BlockPos;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DispenseCapsuleBehavior extends DefaultDispenseItemBehavior {
    protected static final Logger LOGGER = LogManager.getLogger(DispenseCapsuleBehavior.class);

    public ItemStack execute(BlockSource source, ItemStack capsule) {
        if (!(capsule.getItem() instanceof CapsuleItem)) return capsule;

        ServerLevel serverWorld = source.level();
        // capture bases act for the player who placed them; other dispensers, and bases placed before Capsule 9, for nobody
        ServerPlayer actor = source.blockEntity() instanceof BlockEntityCapture base ? Claims.fakePlayer(serverWorld, base.getPlacer()) : null;
        if (CapsuleItem.hasState(capsule, CapsuleItem.CapsuleState.DEPLOYED) && CapsuleItem.getDimension(capsule) != null) {
            try {
                Capsule.resentToCapsule(capsule, serverWorld, null, actor);
                source.level().playSound(null, source.pos(), SoundEvents.STONE_BUTTON_CLICK_OFF, SoundSource.BLOCKS, 0.2F, 0.4F);
            } catch (Exception e) {
                LOGGER.error("Couldn't resend the content into the capsule", e);
            }
        } else if (CapsuleItem.hasStructureLink(capsule)) {
            final int size = CapsuleItem.getSize(capsule);
            final int extendLength = (size - 1) / 2;

            BlockPos anchor = Spacial.getAnchor(source.pos(), source.state(), size).offset(0, CapsuleItem.getYOffset(capsule), 0);
            boolean deployed = Capsule.deployCapsule(capsule, anchor, actor, extendLength, serverWorld);
            if (deployed) {
                source.level().playSound(null, source.pos(), SoundEvents.ARROW_SHOOT, SoundSource.BLOCKS, 0.2F, 0.4F);
                Capsule.showDeployParticules(serverWorld, source.pos(), size);
            }
            if (deployed && CapsuleItem.isOneUse(capsule)) {
                capsule.shrink(1);
            }
        }
        return capsule;
    }

    protected void playSound(BlockSource source) {
        source.level().levelEvent(1000, source.pos(), 0);
    }
}
