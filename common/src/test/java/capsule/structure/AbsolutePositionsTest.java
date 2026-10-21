package capsule.structure;

import capsule.structure.AbsolutePositions.Format;
import capsule.structure.AbsolutePositions.Rule;
import com.google.gson.JsonParser;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
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

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AbsolutePositionsTest {

    private static final BlockPos ORIGIN = new BlockPos(100, 64, 200);
    private static final int SIZE = 5;
    private static final Predicate<BlockPos> INSIDE = BoundingBox.fromCorners(ORIGIN, ORIGIN.offset(SIZE - 1, SIZE - 1, SIZE - 1))::isInside;
    private static final String OVERWORLD = "minecraft:overworld";
    private static final BlockPos DEST = new BlockPos(-30, 70, 12);

    private static CapsuleTemplate template() {
        CapsuleTemplate template = new CapsuleTemplate();
        template.size = new BlockPos(SIZE, SIZE, SIZE);
        return template;
    }

    private static void deploy(CompoundTag nbt, StructurePlaceSettings placement, String dimension) {
        AbsolutePositions.toAbsolute(nbt, relative -> template().deployedPos(relative, DEST, placement), dimension);
    }

    @Test
    void positionInsideIsStoredRelativeThenFollowsTheDeployment() {
        CompoundTag nbt = new CompoundTag();
        nbt.put("flower_pos", NbtUtils.writeBlockPos(ORIGIN.offset(2, 1, 3)));

        AbsolutePositions.toRelative(nbt, Rule.DEFAULT, ORIGIN, INSIDE, OVERWORLD);
        assertEquals(NbtUtils.writeBlockPos(new BlockPos(2, 1, 3)), nbt.get("flower_pos"));
        assertTrue(nbt.contains(AbsolutePositions.RELATIVE_PATHS));

        deploy(nbt, new StructurePlaceSettings(), OVERWORLD);
        assertEquals(NbtUtils.writeBlockPos(DEST.offset(2, 1, 3)), nbt.get("flower_pos"));
        assertFalse(nbt.contains(AbsolutePositions.RELATIVE_PATHS));
    }

    @Test
    void positionOutsideOrLeftInPlaceIsKept() {
        BlockPos leftInPlace = ORIGIN.offset(1, 1, 1);
        CompoundTag nbt = new CompoundTag();
        nbt.put("exit_portal", NbtUtils.writeBlockPos(new BlockPos(1000, 64, 1000)));
        nbt.put("link", NbtUtils.writeBlockPos(leftInPlace));
        CompoundTag before = nbt.copy();

        AbsolutePositions.toRelative(nbt, Rule.DEFAULT, ORIGIN, INSIDE.and(pos -> !pos.equals(leftInPlace)), OVERWORLD);

        assertEquals(before, nbt);
    }

    @Test
    void positionFollowsTheRotationAndMirrorOfTheBlocks() {
        BlockPos templatePos = new BlockPos(0, 2, 1);
        CompoundTag nbt = new CompoundTag();
        nbt.put("target", NbtUtils.writeBlockPos(ORIGIN.offset(templatePos)));
        AbsolutePositions.toRelative(nbt, Rule.DEFAULT, ORIGIN, INSIDE, OVERWORLD);
        StructurePlaceSettings placement = new StructurePlaceSettings().setRotation(Rotation.CLOCKWISE_90).setMirror(Mirror.LEFT_RIGHT);

        deploy(nbt, placement, OVERWORLD);

        // where CapsuleTemplate.spawnBlocksAndEntities places the block of templatePos
        BlockPos block = CapsuleTemplate.processBlockInfos(null, null, DEST, placement, List.of(new StructureTemplate.StructureBlockInfo(templatePos, null, null)))
                .getFirst().pos().offset(CapsuleTemplate.recenterRotation((SIZE - 1) / 2, placement));
        assertEquals(NbtUtils.writeBlockPos(block), nbt.get("target"));
        assertEquals(DEST.offset(1, 2, 0), block);
    }

    @Test
    void globalPosInAnItemFollowsTheDeploymentToAnotherDimension() {
        CompoundTag sameDimension = globalPos(OVERWORLD, ORIGIN.offset(4, 0, 0));
        CompoundTag otherDimension = globalPos("minecraft:the_nether", ORIGIN.offset(4, 0, 0));
        CompoundTag nbt = new CompoundTag();
        ListTag items = new ListTag();
        items.add(compass(sameDimension));
        items.add(compass(otherDimension));
        nbt.put("Items", items);

        AbsolutePositions.toRelative(nbt, Rule.DEFAULT, ORIGIN, INSIDE, OVERWORLD);
        deploy(nbt, new StructurePlaceSettings(), "minecraft:the_end");

        assertEquals(globalPos("minecraft:the_end", DEST.offset(4, 0, 0)), target(nbt, 0));
        assertEquals(otherDimension, target(nbt, 1));
    }

    private static CompoundTag globalPos(String dimension, BlockPos pos) {
        CompoundTag globalPos = new CompoundTag();
        globalPos.putString("dimension", dimension);
        globalPos.put("pos", NbtUtils.writeBlockPos(pos));
        return globalPos;
    }

    private static CompoundTag compass(CompoundTag target) {
        CompoundTag tracker = new CompoundTag();
        tracker.put("target", target.copy());
        CompoundTag components = new CompoundTag();
        components.put("minecraft:lodestone_tracker", tracker);
        CompoundTag item = new CompoundTag();
        item.putString("id", "minecraft:compass");
        item.put("components", components);
        return item;
    }

    private static CompoundTag target(CompoundTag nbt, int slot) {
        return nbt.getList("Items", 10).getCompound(slot).getCompound("components").getCompound("minecraft:lodestone_tracker").getCompound("target");
    }

    @Test
    void xyzCompoundKeepsItsKeys() {
        CompoundTag target = new CompoundTag();
        target.putInt("X", ORIGIN.getX() + 1);
        target.putInt("Y", ORIGIN.getY());
        target.putInt("Z", ORIGIN.getZ() + 2);
        target.putString("side", "up");
        CompoundTag nbt = new CompoundTag();
        nbt.put("Target", target);

        AbsolutePositions.toRelative(nbt, Rule.DEFAULT, ORIGIN, INSIDE, OVERWORLD);
        deploy(nbt, new StructurePlaceSettings(), OVERWORLD);

        CompoundTag deployed = nbt.getCompound("Target");
        assertEquals(DEST.offset(1, 0, 2), new BlockPos(deployed.getInt("X"), deployed.getInt("Y"), deployed.getInt("Z")));
        assertEquals("up", deployed.getString("side"));
    }

    @Test
    void longsArePositionsOnlyUnderTheLongsKeysOfARule() {
        long inside = ORIGIN.offset(1, 2, 3).asLong();
        long outside = new BlockPos(0, -60, 0).asLong();
        CompoundTag nbt = new CompoundTag();
        nbt.putLong("controllerPos", inside);
        nbt.put("pendingPositions", new LongArrayTag(new long[]{inside, outside}));
        ListTag storageKeys = new ListTag();
        CompoundTag storageKey = new CompoundTag();
        storageKey.putLong("position", inside);
        storageKeys.add(storageKey);
        nbt.put("storageKeys", storageKeys);
        nbt.putLong("currentDirection", inside);
        CompoundTag captured = nbt.copy();

        AbsolutePositions.toRelative(captured, Rule.DEFAULT, ORIGIN, INSIDE, OVERWORLD);
        assertEquals(nbt, captured);

        AbsolutePositions.toRelative(captured, new Rule(Rule.DEFAULT.formats(), Set.of("controllerPos", "pendingPositions", "storageKeys"), Set.of()), ORIGIN, INSIDE, OVERWORLD);
        deploy(captured, new StructurePlaceSettings(), OVERWORLD);

        long deployed = DEST.offset(1, 2, 3).asLong();
        assertEquals(deployed, captured.getLong("controllerPos"));
        assertEquals(new LongArrayTag(new long[]{deployed, outside}), captured.get("pendingPositions"));
        assertEquals(deployed, captured.getList("storageKeys", 10).getCompound(0).getLong("position"));
        assertEquals(inside, captured.getLong("currentDirection"));
    }

    @Test
    void selfRepairingMultiblocksKeepTheirControllerAndLastKnownPos() {
        CompoundTag nbt = new CompoundTag();
        nbt.put("LastKnownPos", NbtUtils.writeBlockPos(ORIGIN.offset(1, 0, 1)));
        nbt.put("Controller", NbtUtils.writeBlockPos(ORIGIN));
        CompoundTag before = nbt.copy();

        AbsolutePositions.toRelative(nbt, Rule.DEFAULT, ORIGIN, INSIDE, OVERWORLD);

        assertEquals(before, nbt);
    }

    @Test
    void exceptKeysAreNeverPositions() {
        // Oritech MultiblockMachineController: offsets relative to the machine, inside a capture spanning y = 0
        BlockPos origin = new BlockPos(-2, 0, -2);
        CompoundTag core = new CompoundTag();
        core.putInt("x", 1);
        core.putInt("y", 0);
        core.putInt("z", -1);
        ListTag cores = new ListTag();
        cores.add(core);
        CompoundTag nbt = new CompoundTag();
        nbt.put("connectedCores", cores);
        CompoundTag captured = nbt.copy();
        Predicate<BlockPos> inside = BoundingBox.fromCorners(origin, origin.offset(SIZE - 1, SIZE - 1, SIZE - 1))::isInside;

        AbsolutePositions.toRelative(captured, AbsolutePositions.ruleFor(AbsolutePositions.DEFAULT_RULES, ResourceLocation.parse("oritech:pulverizer_block")), origin, inside, OVERWORLD);
        assertEquals(nbt, captured);

        AbsolutePositions.toRelative(captured, Rule.DEFAULT, origin, inside, OVERWORLD);
        assertTrue(captured.contains(AbsolutePositions.RELATIVE_PATHS), "without the rule, the offset is taken for a position");
    }

    @Test
    void relativePositionsAndOtherIntArraysAreKeptWhenOutsideTheCapture() {
        CompoundTag nbt = new CompoundTag();
        nbt.put("offset", new IntArrayTag(new int[]{1, 0, -2}));
        nbt.put("color", new IntArrayTag(new int[]{255, 128, 0}));
        nbt.put("pattern", new IntArrayTag(new int[]{1, 2, 3, 4}));
        CompoundTag before = nbt.copy();

        AbsolutePositions.toRelative(nbt, Rule.DEFAULT, ORIGIN, INSIDE, OVERWORLD);

        assertEquals(before, nbt);
    }

    @Test
    void aPathRemovedFromTheTemplateIsIgnored() {
        CompoundTag nbt = new CompoundTag();
        nbt.put("flower_pos", NbtUtils.writeBlockPos(ORIGIN));
        AbsolutePositions.toRelative(nbt, Rule.DEFAULT, ORIGIN, INSIDE, OVERWORLD);
        nbt.remove("flower_pos");

        deploy(nbt, new StructurePlaceSettings(), OVERWORLD);

        assertEquals(new CompoundTag(), nbt);
    }

    @Test
    void rulesApplyByBlockThenByNamespace() {
        Map<String, Rule> rules = AbsolutePositions.parseRules(JsonParser.parseString("""
                [
                  {"block": "somemod:teleporter", "longs": ["target"], "except": ["offset"]},
                  {"block": "othermod:", "formats": []},
                  {"block": "broken:block", "formats": ["float"]},
                  {"block": "xyzmod:", "formats": ["xyz"]}
                ]""").getAsJsonArray());

        assertEquals(new Rule(Rule.DEFAULT.formats(), Set.of("target"), Set.of("offset")), AbsolutePositions.ruleFor(rules, ResourceLocation.parse("somemod:teleporter")));
        assertEquals(Rule.DEFAULT, AbsolutePositions.ruleFor(rules, ResourceLocation.parse("somemod:cable")));
        assertEquals(new Rule(EnumSet.noneOf(Format.class), Set.of(), Set.of()), AbsolutePositions.ruleFor(rules, ResourceLocation.parse("othermod:anything")));
        assertEquals(new Rule(EnumSet.of(Format.XYZ), Set.of(), Set.of()), AbsolutePositions.ruleFor(rules, ResourceLocation.parse("xyzmod:anything")));
        assertEquals(Rule.DEFAULT, AbsolutePositions.ruleFor(rules, ResourceLocation.parse("broken:block")));
    }
}
