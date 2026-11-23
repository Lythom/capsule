package capsule.gui;

import capsule.helpers.NBTHelper;
import capsule.network.CapsuleNetwork;
import capsule.network.LabelEditedMessageToServer;
import capsule.platform.Services;
import com.google.common.collect.Lists;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/**
 * @author Lythom
 */
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

    public static void open(Player player) {
        if (player.level().isClientSide()) {
            Minecraft.getInstance().gui.setScreen(new LabelGui(player));
        }
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
                label = tag.getStringOr("label", "");
            }
        }
        textInput.setValue(label);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        super.keyPressed(event);
        if (event.key() == GLFW.GLFW_KEY_ESCAPE || event.key() == GLFW.GLFW_KEY_ENTER) {
            this.onClose();
        }
        if (this.textInput.keyPressed(event)) {
            setCurrentItemLabel(this.textInput.getValue());
        }
        return true;
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        super.charTyped(event);
        if (this.textInput.charTyped(event)) {
            setCurrentItemLabel(this.textInput.getValue());
        }
        return true;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        super.mouseClicked(event, doubleClick);
        return this.textInput.mouseClicked(event, doubleClick);
    }

    public void setCurrentItemLabel(String label) {
        Services.NETWORK.sendToServer(new LabelEditedMessageToServer(label));
    }

    public ItemStack getItemStack() {
        return this.player.getMainHandItem();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTicks);
        // text colors are ARGB since 1.21.6: opaque white
        graphics.centeredText(this.font, this.title, this.width / 2, 70, 0xFFFFFFFF);
        textInput.extractRenderState(graphics, mouseX, mouseY, partialTicks);
    }
}
