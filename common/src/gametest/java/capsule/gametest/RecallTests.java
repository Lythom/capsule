package capsule.gametest;

import capsule.enchantments.CapsuleEnchantments;
import capsule.items.CapsuleItem;
import capsule.items.CapsuleItem.CapsuleState;
import capsule.items.ThrownCapsules;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

import static capsule.gametest.CapsuleTestUtils.assertTrue;
import static capsule.gametest.CapsuleTestUtils.capture;

public class RecallTests {

    private static Holder<Enchantment> enchantment(GameTestHelper helper, ResourceKey<Enchantment> key) {
        return helper.getLevel().registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(key);
    }

    @GameTest(template = "empty")
    public static void enchantingTablesOfferLoyaltyForCapsules(GameTestHelper helper) {
        Registry<Enchantment> enchantments = helper.getLevel().registryAccess().registryOrThrow(Registries.ENCHANTMENT);
        ItemStack capsule = CapsuleTestUtils.emptyCapsule(3);
        RandomSource random = RandomSource.create(0);
        Set<Holder<Enchantment>> offered = new HashSet<>();
        IntStream.range(0, 200).forEach(i -> EnchantmentHelper
                .selectEnchantment(random, capsule, 1 + i % 30, enchantments.getOrCreateTag(EnchantmentTags.IN_ENCHANTING_TABLE).stream())
                .forEach(e -> offered.add(e.enchantment)));

        assertTrue(helper, offered.stream().anyMatch(e -> e.is(Enchantments.LOYALTY)), "an enchanting table should offer loyalty on a capsule");
        assertTrue(helper, offered.stream().allMatch(e -> e.is(Enchantments.LOYALTY)), "only loyalty should be offered on a capsule, got " + offered);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void recallIsNoLongerObtainable(GameTestHelper helper) {
        Holder<Enchantment> recall = enchantment(helper, CapsuleEnchantments.RECALL);
        for (TagKey<Enchantment> tag : List.of(EnchantmentTags.IN_ENCHANTING_TABLE, EnchantmentTags.NON_TREASURE, EnchantmentTags.TRADEABLE,
                EnchantmentTags.ON_RANDOM_LOOT, EnchantmentTags.ON_TRADED_EQUIPMENT, EnchantmentTags.ON_MOB_SPAWN_EQUIPMENT)) {
            assertTrue(helper, !recall.is(tag), "recall should not be in #" + tag.location());
        }
        helper.succeed();
    }

    private static ItemStack anvil(GameTestHelper helper, ItemStack left, Holder<Enchantment> enchantment, int level) {
        ServerPlayer player = CapsuleTestUtils.survivalPlayer(helper, new BlockPos(1, 1, 1));
        AnvilMenu menu = new AnvilMenu(0, player.getInventory(), ContainerLevelAccess.create(helper.getLevel(), helper.absolutePos(new BlockPos(1, 1, 1))));
        menu.getSlot(0).set(left);
        menu.getSlot(1).set(EnchantedBookItem.createForEnchantment(new EnchantmentInstance(enchantment, level)));
        ItemStack result = menu.getSlot(2).getItem().copy();
        CapsuleTestUtils.removePlayer(player);
        return result;
    }

    @GameTest(template = "empty")
    public static void anvilAppliesALoyaltyBookToACapsule(GameTestHelper helper) {
        Holder<Enchantment> loyalty = enchantment(helper, Enchantments.LOYALTY);

        ItemStack result = anvil(helper, CapsuleTestUtils.emptyCapsule(3), loyalty, 3);

        assertTrue(helper, result.getItem() instanceof CapsuleItem && result.getEnchantments().getLevel(loyalty) == 3, "the anvil should give a loyalty III capsule, got " + result);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void otherTridentEnchantmentsAreRefused(GameTestHelper helper) {
        for (ResourceKey<Enchantment> key : List.of(Enchantments.IMPALING, Enchantments.RIPTIDE, Enchantments.CHANNELING)) {
            Holder<Enchantment> enchantment = enchantment(helper, key);
            ItemStack result = anvil(helper, CapsuleTestUtils.emptyCapsule(3), enchantment, 1);
            assertTrue(helper, result.isEmpty(), "the anvil should refuse " + key.location() + " on a capsule, got " + result.getEnchantments());
        }
        helper.succeed();
    }

    private static ItemEntity drop(GameTestHelper helper, ItemStack stack, Vec3 relativePos, ServerPlayer thrower) {
        Vec3 pos = helper.absoluteVec(relativePos);
        ItemEntity entity = new ItemEntity(helper.getLevel(), pos.x, pos.y, pos.z, stack, 0, 0, 0);
        if (thrower != null) entity.setThrower(thrower);
        helper.getLevel().addFreshEntity(entity);
        return entity;
    }

    private static void throwComesBack(GameTestHelper helper, ResourceKey<Enchantment> enchantment) {
        helper.setBlock(1, 1, 1, Blocks.GOLD_BLOCK);
        ItemStack capsule = capture(helper, new BlockPos(1, 1, 1), 1);
        capsule.enchant(enchantment(helper, enchantment), 1);
        CapsuleItem.setState(capsule, CapsuleState.ACTIVATED);
        CapsuleTestUtils.fill(helper, new BlockPos(0, 0, 0), new BlockPos(8, 0, 8), Blocks.STONE.defaultBlockState());
        ServerPlayer player = CapsuleTestUtils.survivalPlayer(helper, new BlockPos(7, 1, 7));

        drop(helper, capsule, new Vec3(4.5, 3, 4.5), player);

        helper.succeedWhen(() -> {
            helper.assertBlockPresent(Blocks.GOLD_BLOCK, new BlockPos(4, 1, 4));
            assertTrue(helper, player.getInventory().contains(s -> CapsuleItem.hasState(s, CapsuleState.DEPLOYED)), "the deployed capsule should be back in the inventory");
            CapsuleTestUtils.removePlayer(player);
        });
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void loyaltyCapsuleComesBack(GameTestHelper helper) {
        throwComesBack(helper, Enchantments.LOYALTY);
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void legacyRecallCapsuleComesBack(GameTestHelper helper) {
        throwComesBack(helper, CapsuleEnchantments.RECALL);
    }

    private static void lavaPool(GameTestHelper helper) {
        CapsuleTestUtils.fill(helper, new BlockPos(0, 0, 0), new BlockPos(8, 0, 8), Blocks.STONE.defaultBlockState());
        CapsuleTestUtils.fill(helper, new BlockPos(2, 1, 2), new BlockPos(6, 1, 6), Blocks.STONE.defaultBlockState());
        CapsuleTestUtils.fill(helper, new BlockPos(3, 1, 3), new BlockPos(5, 1, 5), Blocks.LAVA.defaultBlockState());
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void capsuleThrownIntoLavaDeploysAndComesBack(GameTestHelper helper) {
        helper.setBlock(1, 1, 1, Blocks.GOLD_BLOCK);
        ItemStack capsule = capture(helper, new BlockPos(1, 1, 1), 1);
        capsule.enchant(enchantment(helper, Enchantments.LOYALTY), 1);
        CapsuleItem.setState(capsule, CapsuleState.ACTIVATED);
        lavaPool(helper);
        ServerPlayer player = CapsuleTestUtils.survivalPlayer(helper, new BlockPos(7, 1, 7));

        drop(helper, capsule, new Vec3(4.5, 4, 4.5), player);

        helper.succeedWhen(() -> {
            assertTrue(helper, BlockPos.betweenClosedStream(new BlockPos(3, 1, 3), new BlockPos(5, 3, 5)).anyMatch(p -> helper.getBlockState(p).is(Blocks.GOLD_BLOCK)), "capsule should deploy its gold block in the lava");
            assertTrue(helper, player.getInventory().contains(s -> CapsuleItem.hasState(s, CapsuleState.DEPLOYED)), "the deployed capsule should be back in the inventory");
            CapsuleTestUtils.removePlayer(player);
        });
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void capsuleSurvivesLava(GameTestHelper helper) {
        lavaPool(helper);

        ItemEntity entity = drop(helper, CapsuleTestUtils.emptyCapsule(3), new Vec3(4.5, 2, 4.5), null);

        helper.runAfterDelay(60, () -> {
            assertTrue(helper, entity.isAlive(), "the capsule should not burn in lava");
            entity.discard();
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void droppedLoyaltyTridentIsNotRecalled(GameTestHelper helper) {
        CapsuleTestUtils.fill(helper, new BlockPos(0, 0, 0), new BlockPos(8, 0, 8), Blocks.STONE.defaultBlockState());
        ServerPlayer player = CapsuleTestUtils.survivalPlayer(helper, new BlockPos(7, 1, 7));
        ItemStack trident = new ItemStack(Items.TRIDENT);
        trident.enchant(enchantment(helper, Enchantments.LOYALTY), 3);

        ItemEntity entity = drop(helper, trident, new Vec3(2.5, 3, 2.5), player);

        helper.runAfterDelay(40, () -> {
            assertTrue(helper, entity.isAlive() && entity.onGround(), "the trident should lie on the ground");
            assertTrue(helper, !player.getInventory().contains(new ItemStack(Items.TRIDENT)), "the trident should not be recalled");
            CapsuleTestUtils.removePlayer(player);
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void onlyCapsuleEntitiesAreTracked(GameTestHelper helper) {
        ItemStack trident = new ItemStack(Items.TRIDENT);
        trident.enchant(enchantment(helper, Enchantments.LOYALTY), 1);
        ItemEntity capsule = drop(helper, CapsuleTestUtils.emptyCapsule(3), new Vec3(2.5, 2, 2.5), null);
        List<ItemEntity> others = List.of(
                drop(helper, trident, new Vec3(4.5, 2, 4.5), null),
                drop(helper, new ItemStack(Items.STONE, 64), new Vec3(6.5, 2, 6.5), null));

        List<ItemEntity> tracked = ThrownCapsules.in(helper.getLevel());
        assertTrue(helper, tracked.contains(capsule), "the capsule entity should be tracked");
        assertTrue(helper, others.stream().noneMatch(tracked::contains), "only capsule entities should be tracked");

        capsule.discard();
        assertTrue(helper, !ThrownCapsules.in(helper.getLevel()).contains(capsule), "a removed capsule entity should not be tracked");
        helper.killAllEntities();
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void recallLetsAnEarlyCollidingCapsuleDeploy(GameTestHelper helper) {
        helper.setBlock(1, 1, 1, Blocks.GOLD_BLOCK);
        ItemStack capsule = capture(helper, new BlockPos(1, 1, 1), 1);
        capsule.enchant(enchantment(helper, CapsuleEnchantments.RECALL), 1);
        CapsuleItem.setState(capsule, CapsuleState.ACTIVATED);
        CapsuleTestUtils.fill(helper, new BlockPos(0, 0, 0), new BlockPos(8, 0, 8), Blocks.STONE.defaultBlockState());
        helper.setBlock(5, 3, 5, Blocks.STONE);
        ServerPlayer player = CapsuleTestUtils.survivalPlayer(helper, new BlockPos(7, 1, 7));

        // starts right under a ceiling so that it collides during its first tick
        Vec3 pos = helper.absoluteVec(new Vec3(5.5, 2.7, 5.5));
        ItemEntity entity = new ItemEntity(helper.getLevel(), pos.x, pos.y, pos.z, capsule, 0, 0.3, 0);
        entity.setThrower(player);
        helper.getLevel().addFreshEntity(entity);

        helper.succeedWhen(() -> {
            assertTrue(helper, BlockPos.betweenClosedStream(new BlockPos(0, 1, 0), new BlockPos(8, 4, 8)).anyMatch(p -> helper.getBlockState(p).is(Blocks.GOLD_BLOCK)), "capsule should deploy its gold block");
            CapsuleTestUtils.removePlayer(player);
        });
    }
}
