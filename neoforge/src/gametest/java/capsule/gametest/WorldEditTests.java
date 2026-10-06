package capsule.gametest;

import capsule.StructureSaver;
import capsule.helpers.Capsule;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.BuiltInClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardWriter;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.neoforge.NeoForgeAdapter;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.world.World;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static capsule.gametest.CapsuleTestUtils.assertTrue;

/**
 * Sponge schematics written by WorldEdit's own clipboard writers (#70), and the other fixtures of SchematicTests checked
 * with WorldEdit's readers. The files land in the game directory, folder worldedit-schematics: the fixtures of
 * SchematicTests were made by this test. Registered when WorldEdit is loaded, see docs/TESTING.md.
 */
public class WorldEditTests {

    private static final String FOLDER = "worldedit-schematics";

    @GameTest(template = "empty")
    public static void worldEditSchematicsDeploy(GameTestHelper helper) throws IOException, WorldEditException {
        BlockPos corner = new BlockPos(0, 1, 0);
        SchematicTests.buildScene(helper, corner);
        World world = NeoForgeAdapter.adapt(helper.getLevel());
        BlockPos min = helper.absolutePos(corner);
        CuboidRegion region = new CuboidRegion(world, BlockVector3.at(min.getX(), min.getY(), min.getZ()), BlockVector3.at(min.getX() + 2, min.getY() + 2, min.getZ() + 2));
        BlockArrayClipboard clipboard = new BlockArrayClipboard(region);
        try (EditSession session = WorldEdit.getInstance().newEditSession(world)) {
            ForwardExtentCopy copy = new ForwardExtentCopy(session, region, clipboard, region.getMinimumPoint());
            copy.setCopyingEntities(true);
            Operations.complete(copy);
        }
        Path folder = Files.createDirectories(Path.of(FOLDER));
        write(clipboard, BuiltInClipboardFormat.SPONGE_V2_SCHEMATIC, folder.resolve("sponge_v2.schem"));
        write(clipboard, BuiltInClipboardFormat.SPONGE_V3_SCHEMATIC, folder.resolve("sponge_v3.schem"));
        StructureTemplate vanilla = new StructureTemplate();
        vanilla.fillFromWorld(helper.getLevel(), min, new net.minecraft.core.Vec3i(3, 3, 3), true, Blocks.STRUCTURE_VOID);
        try (OutputStream out = Files.newOutputStream(folder.resolve("vanilla.nbt"))) {
            NbtIo.writeCompressed(vanilla.save(new CompoundTag()), out);
        }
        CapsuleTestUtils.clear(helper, new BlockPos(0, 1, 0), new BlockPos(8, 4, 8));

        for (String name : List.of("sponge_v2", "sponge_v3", "vanilla")) {
            String path = FOLDER + "/" + name;
            StructureSaver.getRewardManager(helper.getLevel().getServer().getResourceManager()).remove(net.minecraft.resources.ResourceLocation.parse(path));
            assertTrue(helper, CapsuleTestUtils.deploy(helper, Capsule.newRewardCapsuleItemStack(path, 0, 0, 3, null, null), new BlockPos(4, 0, 4), null), path + " should deploy");
            List<String> problems = SchematicTests.sceneProblems(helper, new BlockPos(3, 1, 3), true);
            assertTrue(helper, problems.isEmpty(), path + ": " + problems);
            CapsuleTestUtils.clear(helper, new BlockPos(0, 1, 0), new BlockPos(8, 4, 8));
        }

        Map<String, ClipboardFormat> handMade = Map.of(
                "mcedit.schematic", BuiltInClipboardFormat.MCEDIT_SCHEMATIC,
                "sponge_v1.schem", BuiltInClipboardFormat.SPONGE_V1_SCHEMATIC);
        for (Map.Entry<String, ClipboardFormat> fixture : handMade.entrySet()) {
            try (InputStream in = SchematicTests.class.getResourceAsStream("/data/capsule/schematics/" + fixture.getKey());
                 ClipboardReader reader = fixture.getValue().getReader(in)) {
                Clipboard read = reader.read();
                BlockVector3 chest = read.getMinimumPoint().add(1, 1, 1);
                assertTrue(helper, read.getBlock(chest).getBlockType().id().equals("minecraft:chest") && read.getFullBlock(chest).getNbtReference() != null,
                        "WorldEdit should read the chest of " + fixture.getKey() + ", got " + read.getFullBlock(chest));
            }
        }
        helper.succeed();
    }

    private static void write(Clipboard clipboard, ClipboardFormat format, Path file) throws IOException {
        try (ClipboardWriter writer = format.getWriter(Files.newOutputStream(file))) {
            writer.write(clipboard);
        }
    }
}
