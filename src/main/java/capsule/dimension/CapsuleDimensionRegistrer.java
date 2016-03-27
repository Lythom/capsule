package capsule.dimension;

import capsule.Config;
import net.minecraft.world.DimensionType;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.config.Property;

public class CapsuleDimensionRegistrer {

	public static int dimensionId;
	public static DimensionType capsuleDimension;

	public CapsuleDimensionRegistrer() {
		
	}
	
	public static void registerDimension(){
		
		Property providerIdProp = Config.config.get("Compatibility", "dimensionId", 2);
		providerIdProp.setComment("id of the capsule dimension (where blocks are sent inside the capsule).\nChange needed only if there is conflict with an other mod using the same id. This id is used for both the dimension and the dimensionType.");
		CapsuleDimensionRegistrer.dimensionId = providerIdProp.getInt();
		Config.config.save();

		// create new available dimension
		if(!DimensionManager.isDimensionRegistered(CapsuleDimensionRegistrer.dimensionId)){
			capsuleDimension = DimensionType.register("Capsule", "_capsule", CapsuleDimensionRegistrer.dimensionId, CapsuleWorldProvider.class, true);
			DimensionManager.registerDimension(CapsuleDimensionRegistrer.dimensionId, CapsuleDimensionRegistrer.capsuleDimension);
		}
		
	}

}
