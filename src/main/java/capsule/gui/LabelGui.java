package capsule.gui;

import capsule.network.CapsuleNetwork;
import capsule.network.LabelEditedMessageToServer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.glfw.GLFW;

/**
 * @author Lythom
 */
@OnlyIn(Dist.CLIENT)
public class LabelGui extends Screen {
    public static int GUI_WIDTH = 250;
    public static int GUI_HEIGHT = 20;

    private TextFieldWidget textInput;
    private PlayerEntity player;

    public LabelGui(PlayerEntity player) {
        super(new TranslationTextComponent("capsule.gui.capsuleName"));
        this.player = player;
    }

    public void tick() {
        this.textInput.tick();
    }

    public void init() {
        super.init();
        buttons.clear();

        textInput = new TextFieldWidget(this.font, this.width / 2 - GUI_WIDTH / 2, this.height / 2 - GUI_HEIGHT / 2, GUI_WIDTH, GUI_HEIGHT, I18n.format("capsule.gui.capsuleName"));
        textInput.setMaxStringLength(32);
        textInput.changeFocus(true);

        this.addButton(new Button(
                textInput.x + textInput.getWidth() - 200,
                textInput.y + textInput.getHeight() + 10,
                200,
                20,
                I18n.format("gui.done"),
                (p_212984_1_) -> {
                    this.onClose();
                }));

        String label = "";
        ItemStack itemStack = this.getItemStack();
        if (!itemStack.isEmpty() && itemStack.hasTag()) {
            //noinspection ConstantConditions
            label = itemStack.getTag().getString("label");
        }
        textInput.setText(label);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        super.keyPressed(keyCode, scanCode, modifiers);
        if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_ENTER) {
            this.onClose();
        }
        if (this.textInput.keyPressed(keyCode, scanCode, modifiers)) {
            setCurrentItemLabel(this.textInput.getText());
        }
        return true;
    }

    @Override
    public boolean charTyped(char p_charTyped_1_, int p_charTyped_2_) {
        super.charTyped(p_charTyped_1_, p_charTyped_2_);
        if (this.textInput.charTyped(p_charTyped_1_, p_charTyped_2_)) {
            setCurrentItemLabel(this.textInput.getText());
        }
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        return this.textInput.mouseClicked(mouseX, mouseY, mouseButton);
    }

    public void setCurrentItemLabel(String label) {
        CapsuleNetwork.wrapper.sendToServer(new LabelEditedMessageToServer(label));
    }

    public ItemStack getItemStack() {
        return this.player.getHeldItemMainhand();
    }

    @Override
    public void render(int p_render_1_, int p_render_2_, float p_render_3_) {
        this.renderBackground();
        this.drawCenteredString(this.font, this.title.getFormattedText(), this.width / 2, 70, 16777215);
        textInput.render(p_render_1_, p_render_2_, p_render_3_);
        super.render(p_render_1_, p_render_2_, p_render_3_);
    }
}
