package capsule.dimension;

import capsule.Config;
import net.minecraft.world.DimensionType;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.config.Property;

public class CapsuleDimensionRegistrer {

	public static int providerId;
	public static int dimensionId;
	public static DimensionType capsuleDimension;

	public CapsuleDimensionRegistrer() {
		
	}
	
	public static void registerDimension(){
		
		Property providerIdProp = Config.config.get("Compatibility", "providerId", 7);
		providerIdProp.setComment("Provider id of the capsule dimension (where blocks are sent inside the capsule).\nChange needed only if there is conflict with an other mod using the same providerId.");
		CapsuleDimensionRegistrer.providerId = providerIdProp.getInt();
		Config.config.save();
		
		capsuleDimension = DimensionType.register("Capsule", "_capsule", CapsuleDimensionRegistrer.providerId, CapsuleWorldProvider.class, false);
		
		// find first available dimension id
		CapsuleDimensionRegistrer.dimensionId = -1;
		for (Integer id : DimensionManager.getStaticDimensionIDs()) {
			DimensionType dtype = DimensionManager.getProviderType(id);
			if(dtype != null && dtype.getId() == CapsuleDimensionRegistrer.providerId){
				CapsuleDimensionRegistrer.dimensionId = id;
				break;
			}
		}
		
		if(CapsuleDimensionRegistrer.dimensionId == -1){
			CapsuleDimensionRegistrer.dimensionId = DimensionManager.getNextFreeDimId();
			DimensionManager.registerDimension(CapsuleDimensionRegistrer.dimensionId, CapsuleDimensionRegistrer.capsuleDimension);
		}
	}

}
