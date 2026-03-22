package capsule.gui;

import capsule.helpers.NBTHelper;
import capsule.network.CapsuleNetwork;
import capsule.network.LabelEditedMessageToServer;
import com.google.common.collect.Lists;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/**
 * @author Lythom
 */
@OnlyIn(Dist.CLIENT)
public class LabelGui extends Screen {
    public static int GUI_WIDTH = 250;
    public static int GUI_HEIGHT = 20;

    private EditBox textInput;
    private Player player;
    private Component gui_capsule_name = Component.translatable("capsule.gui.capsuleName");
    private final List<Button> buttons = Lists.newArrayList();

    public LabelGui(Player player) {
        super(Component.translatable("capsule.gui.capsuleName"));
        this.player = player;
    }

    public void init() {
        super.init();

        textInput = new EditBox(this.font, this.width / 2 - GUI_WIDTH / 2, this.height / 2 - GUI_HEIGHT / 2, GUI_WIDTH, GUI_HEIGHT, gui_capsule_name);
        textInput.setMaxLength(32);
        textInput.setFocused(true);

        buttons.add(Button.builder(CommonComponents.GUI_DONE, (button) -> this.onClose())
                .bounds(textInput.getX() + textInput.getWidth() - 200,
                        textInput.getY() + textInput.getHeight() + 10,
                        200,
                        20).build());

        String label = "";
        ItemStack itemStack = this.getItemStack();
        if (!itemStack.isEmpty() && NBTHelper.hasTag(itemStack)) {
            CompoundTag tag = NBTHelper.getTag(itemStack);
            if (tag != null) {
                label = tag.getString("label");
            }
        }
        textInput.setValue(label);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        super.keyPressed(keyCode, scanCode, modifiers);
        if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_ENTER) {
            this.onClose();
        }
        if (this.textInput.keyPressed(keyCode, scanCode, modifiers)) {
            setCurrentItemLabel(this.textInput.getValue());
        }
        return true;
    }

    @Override
    public boolean charTyped(char p_charTyped_1_, int p_charTyped_2_) {
        super.charTyped(p_charTyped_1_, p_charTyped_2_);
        if (this.textInput.charTyped(p_charTyped_1_, p_charTyped_2_)) {
            setCurrentItemLabel(this.textInput.getValue());
        }
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        return this.textInput.mouseClicked(mouseX, mouseY, mouseButton);
    }

    public void setCurrentItemLabel(String label) {
        PacketDistributor.sendToServer(new LabelEditedMessageToServer(label));
    }

    public ItemStack getItemStack() {
        return this.player.getMainHandItem();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 70, 16777215);
        textInput.render(guiGraphics, mouseX, mouseY, partialTicks);
    }
}
