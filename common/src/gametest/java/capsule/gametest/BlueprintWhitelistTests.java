package capsule.gametest;

import capsule.Config;
import capsule.StructureSaver;
import capsule.helpers.Blueprint;
import capsule.helpers.Capsule;
import capsule.items.CapsuleItem;
import capsule.items.CapsuleItem.CapsuleState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import net.minecraft.world.level.block.entity.LecternBlockEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static capsule.gametest.CapsuleTestUtils.assertTrue;
import static capsule.gametest.CapsuleTestUtils.capture;

public class BlueprintWhitelistTests {

    /**
     * Vanilla block entity types left out of blueprints, see blueprint_whitelist.json for the others.
     */
    private static final Map<String, String> EXCLUDED = Map.of(
            "minecraft:mob_spawner", "spawners have no survival item",
            "minecraft:trial_spawner", "spawners have no survival item",
            "minecraft:vault", "holds per player loot, no survival item",
            "minecraft:end_portal", "no item",
            "minecraft:end_gateway", "no item",
            "minecraft:piston", "moving piston blocks only exist during a piston move",
            "minecraft:brushable_block", "suspicious sand and gravel hold loot",
            "minecraft:jigsaw", "world generation block without use in a build"
    );

    @GameTest(template = "empty")
    public static void everyVanillaBlockEntityIsWhitelistedOrExcluded(GameTestHelper helper) {
        List<String> unlisted = new ArrayList<>();
        for (BlockEntityType<?> type : BuiltInRegistries.BLOCK_ENTITY_TYPE) {
            ResourceLocation typeId = BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(type);
            if (!typeId.getNamespace().equals("minecraft") || EXCLUDED.containsKey(typeId.toString())) continue;
            BuiltInRegistries.BLOCK.stream()
                    .filter(block -> type.isValid(block.defaultBlockState()))
                    .map(block -> BuiltInRegistries.BLOCK.getKey(block).toString())
                    .filter(id -> !Config.blueprintWhitelist.containsKey(id))
                    .forEach(id -> unlisted.add(id + " (" + typeId + ")"));
        }
        assertTrue(helper, unlisted.isEmpty(), "block entities neither whitelisted nor excluded: " + unlisted);

        List<String> dead = Config.blueprintWhitelist.keySet().stream()
                .filter(id -> id.startsWith("minecraft:"))
                .filter(id -> BuiltInRegistries.BLOCK.getOptional(ResourceLocation.parse(id)).map(block -> !block.defaultBlockState().hasBlockEntity()).orElse(true))
                .toList();
        assertTrue(helper, dead.isEmpty(), "whitelisted ids without a block entity: " + dead);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void blueprintsNeverKeepInventories(GameTestHelper helper) {
        fill(helper, new BlockPos(0, 1, 0), Blocks.SHULKER_BOX, Items.DIAMOND);
        fill(helper, new BlockPos(1, 1, 0), Blocks.CHISELED_BOOKSHELF, Items.BOOK);
        fill(helper, new BlockPos(2, 1, 0), Blocks.DECORATED_POT, Items.DIAMOND);
        fill(helper, new BlockPos(0, 1, 2), Blocks.CRAFTER, Items.DIAMOND);
        helper.setBlock(1, 1, 2, Blocks.CAMPFIRE);
        ((CampfireBlockEntity) helper.getBlockEntity(new BlockPos(1, 1, 2))).placeFood(null, new ItemStack(Items.BEEF), 0);
        helper.setBlock(2, 1, 2, Blocks.LECTERN.defaultBlockState().setValue(LecternBlock.HAS_BOOK, true));
        ((LecternBlockEntity) helper.getBlockEntity(new BlockPos(2, 1, 2))).setBook(new ItemStack(Items.WRITABLE_BOOK));
        List<Block> blocks = List.of(Blocks.SHULKER_BOX, Blocks.CHISELED_BOOKSHELF, Blocks.DECORATED_POT, Blocks.CRAFTER, Blocks.CAMPFIRE, Blocks.LECTERN);
        ItemStack source = capture(helper, new BlockPos(0, 1, 0), 3);

        ServerPlayer player = CapsuleTestUtils.survivalPlayer(helper, new BlockPos(8, 1, 8));
        ItemStack blueprint = Capsule.newLinkedCapsuleItemStack(CapsuleItem.getStructureName(source), 0, 0, 3, false, null, 0);
        CapsuleItem.setBlueprint(blueprint);
        CapsuleItem.setState(blueprint, CapsuleState.DEPLOYED);
        CapsuleItem.duplicateBlueprintTemplate(blueprint, helper.getLevel(), player);
        Map<StructureSaver.ItemStackKey, Integer> materials = Blueprint.getMaterialList(CapsuleTestUtils.template(helper, blueprint), helper.getLevel(), player);
        materials.forEach((key, count) -> player.getInventory().add(key.itemStack.copyWithCount(count)));
        Capsule.reloadBlueprint(blueprint, helper.getLevel(), player);
        assertTrue(helper, CapsuleTestUtils.deploy(helper, blueprint, new BlockPos(5, 0, 5), player), "blueprint deploy should succeed");
        CapsuleTestUtils.removePlayer(player);

        List<String> problems = new ArrayList<>();
        for (BlockPos pos : BlockPos.betweenClosed(new BlockPos(4, 1, 4), new BlockPos(6, 1, 6))) {
            BlockEntity be = helper.getLevel().getBlockEntity(helper.absolutePos(pos));
            if (be == null) continue;
            boolean empty = be instanceof Container container ? container.isEmpty()
                    : be instanceof CampfireBlockEntity campfire ? campfire.getItems().stream().allMatch(ItemStack::isEmpty)
                    : !(be instanceof LecternBlockEntity lectern) || !lectern.hasBook();
            if (!empty) problems.add(BuiltInRegistries.BLOCK.getKey(be.getBlockState().getBlock()) + " kept its content");
        }
        for (Block block : blocks) {
            if (BlockPos.betweenClosedStream(new BlockPos(4, 1, 4), new BlockPos(6, 1, 6)).noneMatch(p -> helper.getBlockState(p).is(block))) {
                problems.add(BuiltInRegistries.BLOCK.getKey(block) + " was not deployed");
            }
        }
        assertTrue(helper, problems.isEmpty(), "blueprint problems: " + problems);
        helper.succeed();
    }

    private static void fill(GameTestHelper helper, BlockPos pos, Block block, Item item) {
        helper.setBlock(pos, block);
        Container container = (Container) helper.getBlockEntity(pos);
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            container.setItem(slot, new ItemStack(item));
        }
    }
}
