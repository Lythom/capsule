package capsule;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * Server-wide counter that seeds the names of new capsule and blueprint templates.
 *
 * @author Lythom
 */
public class CapsuleSavedData extends SavedData {

    public static final Codec<CapsuleSavedData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.optionalFieldOf("counter", 0).forGetter(d -> d.capsuleCounter)
    ).apply(i, CapsuleSavedData::new));

    // No data fixer: both loaders skip the fix when it is null. Saved in the server storage, data/capsule/capsule_data.dat;
    // the per-dimension data/capsuleData.dat of older versions is not read, which is harmless because the names taken
    // from the counter are checked against the existing templates.
    public static final SavedDataType<CapsuleSavedData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(CapsuleMod.MODID, "capsule_data"), CapsuleSavedData::new, CODEC, null);

    private int capsuleCounter;

    public CapsuleSavedData() {
        this(0);
    }

    private CapsuleSavedData(int counter) {
        this.capsuleCounter = counter;
    }

    public int getNextCount() {
        this.setDirty();
        return capsuleCounter++;
    }

}
