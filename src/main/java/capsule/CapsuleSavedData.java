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
    public void load(CompoundNBT nbt) {
        // retrieve lastReservedPosition
        capsuleCounter = nbt.getInt("counter");
    }

    @Override
    public CompoundNBT save(CompoundNBT nbt) {
        nbt.putInt("counter", capsuleCounter);
        return nbt;
    }

    public int getNextCount() {
        this.setDirty();
        return capsuleCounter++;
    }

}
