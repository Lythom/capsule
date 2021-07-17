package capsule.blocks;

import net.minecraft.block.*;
import net.minecraft.block.material.Material;
import net.minecraft.block.material.MaterialColor;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.Property;
import net.minecraft.state.StateContainer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.world.IBlockReader;
import net.minecraftforge.common.ToolType;

public class BlockCapsuleMarker extends DispenserBlock {
    /**
     * Whether this fence connects in the northern direction
     */
    public static final Property<Boolean> PROJECTING = BooleanProperty.create("projecting");

    public BlockCapsuleMarker() {
        super(BlockCapsuleMarker.Properties.of(Material.STONE, MaterialColor.STONE)
                .strength(3.5F)
                .sound(SoundType.STONE)
                .harvestTool(ToolType.PICKAXE)
                .harvestLevel(0));

        this.registerDefaultState(this.stateDefinition.any()
                .setValue(PROJECTING, Boolean.FALSE)
        );
    }

    @Override
    protected void createBlockStateDefinition(StateContainer.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(PROJECTING);
    }

    @Override
    public TileEntity newBlockEntity(IBlockReader worldIn) {
        return new TileEntityCapture();
    }
}
