package capsule;

import java.util.List;
import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraft.world.gen.structure.template.Template;
import net.minecraft.world.gen.structure.template.TemplateManager;

public class StructureSaver {

	public static boolean store(WorldServer worldserver, String playerID, String capsuleStructureId, BlockPos position,
			int size, List<Block> excluded, Map<BlockPos, Block> excludedPositions) {

		// TODO : ignore excluded block from save;
		// TODO : ignore excludedPositions from save (and removal)
		
		boolean includeEntities = false;
		Block ignoredBlock = Blocks.STRUCTURE_VOID;

		MinecraftServer minecraftserver = worldserver.getMinecraftServer();
		TemplateManager templatemanager = worldserver.getStructureTemplateManager();
		Template template = templatemanager.getTemplate(minecraftserver, new ResourceLocation(capsuleStructureId));
		template.takeBlocksFromWorld(worldserver, position, new BlockPos(size, size, size), includeEntities,
				ignoredBlock);
		template.setAuthor(playerID);
		return templatemanager.writeTemplate(minecraftserver, new ResourceLocation(capsuleStructureId));

	}

}
