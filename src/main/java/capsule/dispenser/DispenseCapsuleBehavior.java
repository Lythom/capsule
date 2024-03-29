package capsule.dispenser;

import capsule.helpers.Capsule;
import capsule.helpers.Spacial;
import capsule.items.CapsuleItem;
import net.minecraft.block.DispenserBlock;
import net.minecraft.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.dispenser.IBlockSource;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Direction;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.server.ServerWorld;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DispenseCapsuleBehavior extends DefaultDispenseItemBehavior {
    protected static final Logger LOGGER = LogManager.getLogger(DispenseCapsuleBehavior.class);

    public ItemStack execute(IBlockSource source, ItemStack capsule) {
        if (!(capsule.getItem() instanceof CapsuleItem)) return capsule;

        ServerWorld serverWorld = source.getLevel();
        if (CapsuleItem.hasState(capsule, CapsuleItem.CapsuleState.DEPLOYED) && CapsuleItem.getDimension(capsule) != null) {
            try {
                Capsule.resentToCapsule(capsule, serverWorld, null);
                source.getLevel().playSound(null, source.getPos(), SoundEvents.STONE_BUTTON_CLICK_OFF, SoundCategory.BLOCKS, 0.2F, 0.4F);
            } catch (Exception e) {
                LOGGER.error("Couldn't resend the content into the capsule", e);
            }
        } else if (CapsuleItem.hasStructureLink(capsule)) {
            final int size = CapsuleItem.getSize(capsule);
            final int extendLength = (size - 1) / 2;

            BlockPos anchor = Spacial.getAnchor(source.getPos(), source.getBlockState(), size);
            boolean deployed = Capsule.deployCapsule(capsule, anchor, null, extendLength, serverWorld);
            if (deployed) {
                source.getLevel().playSound(null, source.getPos(), SoundEvents.ARROW_SHOOT, SoundCategory.BLOCKS, 0.2F, 0.4F);
                Capsule.showDeployParticules(serverWorld, source.getPos(), size);
            }
            if (deployed && CapsuleItem.isOneUse(capsule)) {
                capsule.shrink(1);
            }
        }
        return capsule;
    }

    protected void playSound(IBlockSource source) {
        source.getLevel().levelEvent(1000, source.getPos(), 0);
    }
}
