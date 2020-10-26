package capsule;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.world.storage.WorldSavedData;

/**
 * @author Lythom
 */
public class CapsuleSavedData extends WorldSavedData {

    private int capsuleCounter = 0;

    public CapsuleSavedData() {
        super("capsuleData");
    }

    @Override
    public void read(CompoundNBT nbt) {
        // retrieve lastReservedPosition
        capsuleCounter = nbt.getInt("counter");
    }

    @Override
    public CompoundNBT write(CompoundNBT nbt) {
        nbt.putInt("counter", capsuleCounter);
        return nbt;
    }

    public int getNextCount() {
        this.markDirty();
        return capsuleCounter++;
    }

}
