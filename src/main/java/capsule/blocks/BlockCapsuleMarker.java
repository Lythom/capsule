package capsule.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.dispenser.DispenseItemBehavior;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.core.dispenser.BlockSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class BlockCapsuleMarker extends DispenserBlock {
    private static final Logger LOGGER = LogManager.getLogger(BlockCapsuleMarker.class);

    public BlockCapsuleMarker() {
        super(BlockCapsuleMarker.Properties.of().mapColor(MapColor.STONE)
                .strength(2.5F)
                .sound(SoundType.STONE));

        this.registerDefaultState(this.stateDefinition.any());
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pPos, BlockState pState) {
        return new BlockEntityCapture(pPos, pState);
    }

    /**
     * Override dispenseFrom to use our custom block entity type (MARKER_TE)
     * instead of vanilla's BlockEntityType.DISPENSER which doesn't match.
     */
    @Override
    protected void dispenseFrom(ServerLevel level, BlockState state, BlockPos pos) {
        DispenserBlockEntity dispenserBlockEntity = level.getBlockEntity(pos, CapsuleBlocks.MARKER_TE.get()).orElse(null);
        if (dispenserBlockEntity == null) {
            LOGGER.warn("Ignoring dispensing attempt for CapsuleMarker without matching block entity at {}", pos);
        } else {
            BlockSource blocksource = new BlockSource(level, pos, state, dispenserBlockEntity);
            int i = dispenserBlockEntity.getRandomSlot(level.random);
            if (i < 0) {
                level.levelEvent(1001, pos, 0);
                level.gameEvent(GameEvent.BLOCK_ACTIVATE, pos, GameEvent.Context.of(dispenserBlockEntity.getBlockState()));
            } else {
                ItemStack itemstack = dispenserBlockEntity.getItem(i);
                DispenseItemBehavior dispenseitembehavior = this.getDispenseMethod(level, itemstack);
                if (dispenseitembehavior != DispenseItemBehavior.NOOP) {
                    dispenserBlockEntity.setItem(i, dispenseitembehavior.dispense(blocksource, itemstack));
                }
            }
        }
    }

    public void neighborChanged(BlockState pState, Level pLevel, BlockPos pPos, Block pBlock, BlockPos pFromPos, boolean pIsMoving) {
        boolean flag = pLevel.hasNeighborSignal(pPos) || pLevel.hasNeighborSignal(pPos.above());
        boolean flag1 = pState.getValue(TRIGGERED);
        if (flag && !flag1) {
            pLevel.scheduleTick(pPos, this, 4);
            pLevel.setBlock(pPos, pState.setValue(TRIGGERED, Boolean.TRUE), 4);
        } else if (!flag && flag1) {
            pLevel.setBlock(pPos, pState.setValue(TRIGGERED, Boolean.FALSE), 4);
        }

    }
}
