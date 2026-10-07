package capsule.gametest;

import capsule.items.CapsuleItem;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.item.ItemInput;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.List;

import static capsule.gametest.CapsuleTestUtils.assertTrue;
import static capsule.gametest.CapsuleTestUtils.capture;

public class CommandTests {

    /**
     * The give command printed by /capsule exportHeldItem gives the held capsule back.
     */
    @GameTest(template = "empty")
    public static void exportedItemsCanBeGivenBack(GameTestHelper helper) throws CommandSyntaxException {
        helper.setBlock(1, 1, 1, Blocks.STONE);
        ItemStack capsule = capture(helper, new BlockPos(1, 1, 1), 1);
        CapsuleItem.setLabel(capsule, "Exported");
        List<Component> messages = new ArrayList<>();
        ServerPlayer player = CapsuleTestUtils.survivalPlayer(helper, new BlockPos(4, 1, 4), messages);
        try {
            player.setItemInHand(InteractionHand.MAIN_HAND, capsule);
            run(helper, player, "capsule exportHeldItem");
            String command = messages.getLast().getString().substring(1);
            ParseResults<CommandSourceStack> give = helper.getLevel().getServer().getCommands().getDispatcher().parse(command, source(player));
            CommandSyntaxException error = Commands.getParseException(give);
            assertTrue(helper, error == null, "/" + command + " is refused: " + (error == null ? "" : error.getMessage()));
            ItemStack given = give.getContext().build(command).getArgument("item", ItemInput.class).createItemStack(1, false);
            assertTrue(helper, ItemStack.isSameItemSameComponents(given, capsule), command + " gives " + given.getComponentsPatch() + " instead of " + capsule.getComponentsPatch());
        } finally {
            CapsuleTestUtils.removePlayer(player);
        }
        helper.succeed();
    }

    private static CommandSourceStack source(ServerPlayer player) {
        return player.createCommandSourceStack().withPermission(2);
    }

    private static void run(GameTestHelper helper, ServerPlayer player, String command) throws CommandSyntaxException {
        helper.getLevel().getServer().getCommands().getDispatcher().execute(command, source(player));
    }
}
