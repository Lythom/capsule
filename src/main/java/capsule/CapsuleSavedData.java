/**
 *
 */
package capsule;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.WorldSavedData;

/**
 * @author Lythom
 */
public class CapsuleSavedData extends WorldSavedData {

    private int capsuleCounter = 0;

    /**
     * @param name
     */
    public CapsuleSavedData(String name) {
        super(name);
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        // retrieve lastReservedPosition
        capsuleCounter = nbt.getInteger("counter");
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        nbt.setInteger("counter", capsuleCounter);
        return nbt;
    }

    public int getNextCount() {
        this.markDirty();
        return capsuleCounter++;
    }

}
