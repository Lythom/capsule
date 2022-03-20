package capsule;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * @author Lythom
 */
public class CapsuleSavedData extends SavedData {

    private int capsuleCounter = 0;

    public CapsuleSavedData() {
        super("capsuleData");
    }

    @Override
    public void load(CompoundTag nbt) {
        // retrieve lastReservedPosition
        capsuleCounter = nbt.getInt("counter");
    }

    @Override
    public CompoundTag save(CompoundTag nbt) {
        nbt.putInt("counter", capsuleCounter);
        return nbt;
    }

    public int getNextCount() {
        this.setDirty();
        return capsuleCounter++;
    }

}
