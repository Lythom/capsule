package capsule.dispenser;

import capsule.blocks.BlockEntityCapture;
import capsule.helpers.Capsule;
import capsule.helpers.Spacial;
import capsule.items.CapsuleItem;
import capsule.plugins.claims.Claims;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockSource;
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

        ServerLevel serverWorld = source.getLevel();
        // capture bases act for the player who placed them; other dispensers, and older bases, for nobody
        ServerPlayer actor = source.getEntity() instanceof BlockEntityCapture base ? Claims.fakePlayer(serverWorld, base.getPlacer()) : null;
        if (CapsuleItem.hasState(capsule, CapsuleItem.CapsuleState.DEPLOYED) && CapsuleItem.getDimension(capsule) != null) {
            try {
                Capsule.resentToCapsule(capsule, serverWorld, null, actor);
                source.getLevel().playSound(null, source.getPos(), SoundEvents.STONE_BUTTON_CLICK_OFF, SoundSource.BLOCKS, 0.2F, 0.4F);
            } catch (Exception e) {
                LOGGER.error("Couldn't resend the content into the capsule", e);
            }
        } else if (CapsuleItem.hasStructureLink(capsule)) {
            final int size = CapsuleItem.getSize(capsule);
            final int extendLength = (size - 1) / 2;

            BlockPos anchor = Spacial.getAnchor(source.getPos(), source.getBlockState(), size).offset(0, CapsuleItem.getYOffset(capsule), 0);
            boolean deployed = Capsule.deployCapsule(capsule, anchor, actor, extendLength, serverWorld);
            if (deployed) {
                source.getLevel().playSound(null, source.getPos(), SoundEvents.ARROW_SHOOT, SoundSource.BLOCKS, 0.2F, 0.4F);
                Capsule.showDeployParticules(serverWorld, source.getPos(), size);
            }
            if (deployed && CapsuleItem.isOneUse(capsule)) {
                capsule.shrink(1);
            }
        }
        return capsule;
    }

    protected void playSound(BlockSource source) {
        source.getLevel().levelEvent(1000, source.getPos(), 0);
    }
}
