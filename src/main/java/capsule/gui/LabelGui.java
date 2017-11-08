package capsule.gui;

import capsule.CommonProxy;
import capsule.network.LabelEditedMessageToServer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import java.io.IOException;

/**
 * @author Lythom
 */
public class LabelGui extends GuiScreen {
    public static int GUI_WIDTH = 250;
    public static int GUI_HEIGHT = 20;

    private GuiTextField textInput;
    private EntityPlayer player;
    private GuiButton buttonDone;

    public LabelGui(EntityPlayer player) {
        this.player = player;
    }

    @Override
    public void initGui() {
        buttonList.clear();
        Keyboard.enableRepeatEvents(true);

        textInput = new GuiTextField(0, this.fontRendererObj, this.width / 2 - GUI_WIDTH / 2, this.height / 2 - GUI_HEIGHT / 2, GUI_WIDTH, GUI_HEIGHT);
        textInput.setMaxStringLength(32);
        textInput.setFocused(true);


        buttonDone = new GuiButton(1, textInput.xPosition + textInput.width - 98, textInput.yPosition + textInput.height + 10, 98, 20, I18n.format("gui.done", new Object[0]));
        buttonDone.enabled = true;
        buttonList.add(buttonDone);

        String label = "";
        ItemStack itemStack = this.getItemStack();
        if (itemStack != null && itemStack.hasTagCompound()) {
            //noinspection ConstantConditions
            label = itemStack.getTagCompound().getString("label");
        }
        textInput.setText(label);


    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        super.keyTyped(typedChar, keyCode);
        if (keyCode == Keyboard.KEY_RETURN) {
            closeGui();
        }
        this.textInput.textboxKeyTyped(typedChar, keyCode);
        setCuurentItemLabel(this.textInput.getText());
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        this.textInput.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public void actionPerformed(GuiButton button) {
        if (button == buttonDone) {
            closeGui();
        }
    }

    private void closeGui() {
        this.mc.displayGuiScreen(null);
        if (this.mc.currentScreen == null) {
            this.mc.setIngameFocus();
        }
    }

    /**
     * Called when the screen is unloaded. Used to disable keyboard repeat
     * events
     */
    @Override
    public void onGuiClosed() {

    }

    @Override
    public void updateScreen() {
        super.updateScreen();
    }

    public void setCuurentItemLabel(String label) {
        CommonProxy.simpleNetworkWrapper.sendToServer(new LabelEditedMessageToServer(label));
    }

    public ItemStack getItemStack() {
        return this.player.getHeldItemMainhand();
    }

    /**
     * Returns true if this GUI should pause the game when it is displayed in
     * single-player
     */
    @Override
    public boolean doesGuiPauseGame() {
        return true;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {

        GL11.glColor4f(1, 1, 1, 1);
        if (this.mc != null) {
            drawDefaultBackground();
        }

        if (this.textInput != null) {
            this.textInput.drawTextBox();
        }

        super.drawScreen(mouseX, mouseY, partialTicks);


    }
}
