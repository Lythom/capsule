package capsule.structure;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AbsolutePositionsTest {

    private static final BlockPos ORIGIN = new BlockPos(100, 64, 200);
    private static final int SIZE = 5;
    private static final Predicate<BlockPos> INSIDE = BoundingBox.fromCorners(ORIGIN, ORIGIN.offset(SIZE - 1, SIZE - 1, SIZE - 1))::isInside;
    private static final String OVERWORLD = "minecraft:overworld";
    private static final BlockPos DEST = new BlockPos(-30, 70, 12);
    private static final BlockPos INSIDE_POS = ORIGIN.offset(2, 1, 3);
    private static final BlockPos DEPLOYED_POS = DEST.offset(2, 1, 3);

    private static CapsuleTemplate template() {
        CapsuleTemplate template = new CapsuleTemplate();
        template.size = new BlockPos(SIZE, SIZE, SIZE);
        return template;
    }

    private static void capture(CompoundTag nbt, String... paths) {
        AbsolutePositions.toRelative(nbt, List.of(paths), ORIGIN, INSIDE, OVERWORLD);
    }

    private static void deploy(CompoundTag nbt, StructurePlaceSettings placement, String dimension) {
        AbsolutePositions.toAbsolute(nbt, relative -> template().deployedPos(relative, DEST, placement), dimension);
    }

    private static void deploy(CompoundTag nbt) {
        deploy(nbt, new StructurePlaceSettings(), OVERWORLD);
    }

    private static CompoundTag xyz(String x, String y, String z, BlockPos pos) {
        CompoundTag tag = new CompoundTag();
        tag.putInt(x, pos.getX());
        tag.putInt(y, pos.getY());
        tag.putInt(z, pos.getZ());
        return tag;
    }

    private static CompoundTag globalPos(String dimension, BlockPos pos) {
        CompoundTag tag = new CompoundTag();
        tag.putString("dimension", dimension);
        tag.put("pos", NbtUtils.writeBlockPos(pos));
        return tag;
    }

    @Test
    void aBlockWithoutRuleKeepsItsTag() {
        Map<String, List<String>> rules = Map.of("minecraft:beehive", List.of("flower_pos"));
        CompoundTag nbt = new CompoundTag();
        nbt.put("flower_pos", NbtUtils.writeBlockPos(INSIDE_POS));
        CompoundTag before = nbt.copy();

        AbsolutePositions.toRelative(nbt, AbsolutePositions.pathsFor(rules, ResourceLocation.parse("minecraft:chest")), ORIGIN, INSIDE, OVERWORLD);

        assertEquals(before, nbt);
    }

    @Test
    void onlyTheListedKeysAreConverted() {
        CompoundTag nbt = new CompoundTag();
        nbt.put("flower_pos", NbtUtils.writeBlockPos(INSIDE_POS));
        nbt.put("other_pos", NbtUtils.writeBlockPos(INSIDE_POS));

        capture(nbt, "flower_pos");
        assertEquals(NbtUtils.writeBlockPos(new BlockPos(2, 1, 3)), nbt.get("flower_pos"));
        assertEquals(NbtUtils.writeBlockPos(INSIDE_POS), nbt.get("other_pos"));

        deploy(nbt);
        assertEquals(NbtUtils.writeBlockPos(DEPLOYED_POS), nbt.get("flower_pos"));
        assertEquals(NbtUtils.writeBlockPos(INSIDE_POS), nbt.get("other_pos"));
        assertFalse(nbt.contains(AbsolutePositions.RELATIVE_PATHS));
    }

    @Test
    void anIdRuleWinsOverANamespaceRule() {
        Map<String, List<String>> rules = AbsolutePositions.parseRules(JsonParser.parseString("""
                [
                  {"id": "sophisticatedstorage:", "positions": ["controllerPos"]},
                  {"id": "sophisticatedstorage:controller", "positions": ["storagePositions[]"]},
                  {"id": "broken:block"}
                ]""").getAsJsonArray());

        assertEquals(List.of("storagePositions[]"), AbsolutePositions.pathsFor(rules, ResourceLocation.parse("sophisticatedstorage:controller")));
        assertEquals(List.of("controllerPos"), AbsolutePositions.pathsFor(rules, ResourceLocation.parse("sophisticatedstorage:chest")));
        assertEquals(List.of(), AbsolutePositions.pathsFor(rules, ResourceLocation.parse("broken:block")));
        assertEquals(List.of(), AbsolutePositions.pathsFor(rules, ResourceLocation.parse("minecraft:chest")));
    }

    @Test
    void everyFormatUnderAListedKeyIsConverted() {
        BlockPos outside = new BlockPos(0, -60, 0);
        CompoundTag nbt = new CompoundTag();
        nbt.put("intArray", NbtUtils.writeBlockPos(INSIDE_POS));
        nbt.put("lowerXyz", xyz("x", "y", "z", INSIDE_POS));
        nbt.put("upperXyz", xyz("X", "Y", "Z", INSIDE_POS));
        nbt.putLong("long", INSIDE_POS.asLong());
        nbt.put("longArray", new LongArrayTag(new long[]{INSIDE_POS.asLong(), outside.asLong()}));
        nbt.put("globalPos", globalPos(OVERWORLD, INSIDE_POS));
        nbt.put("otherDimension", globalPos("minecraft:the_nether", INSIDE_POS));

        capture(nbt, "intArray", "lowerXyz", "upperXyz", "long", "longArray", "globalPos", "otherDimension");
        deploy(nbt, new StructurePlaceSettings(), "minecraft:the_end");

        assertEquals(NbtUtils.writeBlockPos(DEPLOYED_POS), nbt.get("intArray"));
        assertEquals(xyz("x", "y", "z", DEPLOYED_POS), nbt.get("lowerXyz"));
        assertEquals(xyz("X", "Y", "Z", DEPLOYED_POS), nbt.get("upperXyz"));
        assertEquals(DEPLOYED_POS.asLong(), nbt.getLong("long"));
        assertEquals(new LongArrayTag(new long[]{DEPLOYED_POS.asLong(), outside.asLong()}), nbt.get("longArray"));
        assertEquals(globalPos("minecraft:the_end", DEPLOYED_POS), nbt.get("globalPos"));
        assertEquals(globalPos("minecraft:the_nether", INSIDE_POS), nbt.get("otherDimension"));
    }

    @Test
    void pathsGoThroughListsAndKeyPrefixes() {
        CompoundTag nbt = new CompoundTag();
        ListTag pedestals = new ListTag();
        for (BlockPos pos : List.of(INSIDE_POS, ORIGIN)) {
            CompoundTag pedestal = new CompoundTag();
            pedestal.put("blockPos", NbtUtils.writeBlockPos(pos));
            pedestals.add(pedestal);
        }
        nbt.put("sourcePedestals", pedestals);
        CompoundTag connection = new CompoundTag();
        connection.put("Pos", NbtUtils.writeBlockPos(ORIGIN));
        connection.put("Pos2", NbtUtils.writeBlockPos(INSIDE_POS));
        nbt.put("Connection0", connection);
        nbt.putInt("ConnectionCount", 1);

        capture(nbt, "sourcePedestals[].blockPos", "Connection*.Pos", "Connection*.Pos2");
        deploy(nbt);

        ListTag deployed = nbt.getList("sourcePedestals", 10);
        assertEquals(NbtUtils.writeBlockPos(DEPLOYED_POS), deployed.getCompound(0).get("blockPos"));
        assertEquals(NbtUtils.writeBlockPos(DEST), deployed.getCompound(1).get("blockPos"));
        assertEquals(NbtUtils.writeBlockPos(DEST), nbt.getCompound("Connection0").get("Pos"));
        assertEquals(NbtUtils.writeBlockPos(DEPLOYED_POS), nbt.getCompound("Connection0").get("Pos2"));
        assertEquals(1, nbt.getInt("ConnectionCount"));
    }

    @Test
    void itemsAreConvertedOnlyUnderAListedKey() {
        CompoundTag tracker = new CompoundTag();
        tracker.put("target", globalPos(OVERWORLD, INSIDE_POS));
        CompoundTag components = new CompoundTag();
        components.put("minecraft:lodestone_tracker", tracker);
        CompoundTag compass = new CompoundTag();
        compass.putString("id", "minecraft:compass");
        compass.put("components", components);
        ListTag items = new ListTag();
        items.add(compass);
        CompoundTag nbt = new CompoundTag();
        nbt.put("Items", items);
        CompoundTag before = nbt.copy();

        capture(nbt, "flower_pos");
        assertEquals(before, nbt);

        capture(nbt, "Items[].components.minecraft:lodestone_tracker.target");
        deploy(nbt);
        assertEquals(globalPos(OVERWORLD, DEPLOYED_POS), nbt.getList("Items", 10).getCompound(0).getCompound("components").getCompound("minecraft:lodestone_tracker").get("target"));
    }

    @Test
    void positionOutsideOrLeftInPlaceIsKept() {
        BlockPos leftInPlace = ORIGIN.offset(1, 1, 1);
        CompoundTag nbt = new CompoundTag();
        nbt.put("exit_portal", NbtUtils.writeBlockPos(new BlockPos(1000, 64, 1000)));
        nbt.put("link", NbtUtils.writeBlockPos(leftInPlace));
        CompoundTag before = nbt.copy();

        AbsolutePositions.toRelative(nbt, List.of("exit_portal", "link"), ORIGIN, INSIDE.and(pos -> !pos.equals(leftInPlace)), OVERWORLD);

        assertEquals(before, nbt);
    }

    @Test
    void positionFollowsTheRotationAndMirrorOfTheBlocks() {
        BlockPos templatePos = new BlockPos(0, 2, 1);
        CompoundTag nbt = new CompoundTag();
        nbt.put("target", NbtUtils.writeBlockPos(ORIGIN.offset(templatePos)));
        capture(nbt, "target");
        StructurePlaceSettings placement = new StructurePlaceSettings().setRotation(Rotation.CLOCKWISE_90).setMirror(Mirror.LEFT_RIGHT);

        deploy(nbt, placement, OVERWORLD);

        // where CapsuleTemplate.spawnBlocksAndEntities places the block of templatePos
        BlockPos block = CapsuleTemplate.processBlockInfos(null, null, DEST, placement, List.of(new StructureTemplate.StructureBlockInfo(templatePos, null, null)))
                .getFirst().pos().offset(CapsuleTemplate.recenterRotation((SIZE - 1) / 2, placement));
        assertEquals(NbtUtils.writeBlockPos(block), nbt.get("target"));
        assertEquals(DEST.offset(1, 2, 0), block);
    }

    @Test
    void positionsOfBlocksThatStayedBecomeAbsoluteAgain() {
        BlockPos stayed = ORIGIN.offset(1, 0, 0);
        CompoundTag nbt = new CompoundTag();
        nbt.put("a", NbtUtils.writeBlockPos(stayed));
        nbt.put("b", NbtUtils.writeBlockPos(INSIDE_POS));
        capture(nbt, "a", "b");

        AbsolutePositions.keepAbsolute(nbt, ORIGIN, stayed.subtract(ORIGIN)::equals);
        deploy(nbt);

        assertEquals(NbtUtils.writeBlockPos(stayed), nbt.get("a"));
        assertEquals(NbtUtils.writeBlockPos(DEPLOYED_POS), nbt.get("b"));
    }

    @Test
    void relativePositionsNeverIdentifyABlock() {
        CompoundTag first = new CompoundTag();
        first.put("target", NbtUtils.writeBlockPos(INSIDE_POS));
        CompoundTag second = new CompoundTag();
        second.put("target", NbtUtils.writeBlockPos(ORIGIN));
        capture(first, "target");
        capture(second, "target");

        assertEquals(AbsolutePositions.withoutRelativePositions(first), AbsolutePositions.withoutRelativePositions(second));
        assertTrue(first.contains(AbsolutePositions.RELATIVE_PATHS), "the template keeps its relative positions");
    }

    @Test
    void aPathRemovedFromTheTemplateIsIgnored() {
        CompoundTag nbt = new CompoundTag();
        nbt.put("flower_pos", NbtUtils.writeBlockPos(ORIGIN));
        capture(nbt, "flower_pos");
        nbt.remove("flower_pos");

        deploy(nbt);

        assertEquals(new CompoundTag(), nbt);
    }

    @Test
    void theBundledRulesAreValid() throws Exception {
        try (InputStream stream = AbsolutePositions.class.getResourceAsStream("/data/capsule/initialconfig/root/position_rules.json")) {
            assertNotNull(stream);
            JsonArray json = JsonParser.parseReader(new InputStreamReader(stream)).getAsJsonArray();
            Map<String, List<String>> rules = AbsolutePositions.parseRules(json);
            assertEquals(json.size(), rules.size());
            assertEquals(List.of("flower_pos", "bees[].entity_data.flower_pos"), AbsolutePositions.pathsFor(rules, ResourceLocation.parse("minecraft:beehive")));
            assertEquals(List.of("controllerPos"), AbsolutePositions.pathsFor(rules, ResourceLocation.parse("sophisticatedstorage:limited_barrel_1")));
        }
    }
}
