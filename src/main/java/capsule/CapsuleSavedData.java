package capsule;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * @author Lythom
 */
public class CapsuleSavedData extends SavedData {

    private int capsuleCounter = 0;

    public CapsuleSavedData() {
        super();
    }

    public static CapsuleSavedData load(CompoundTag nbt) {
        // retrieve lastReservedPosition
        CapsuleSavedData data = new CapsuleSavedData();
        data.capsuleCounter = nbt.getInt("counter");
        return data;
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
