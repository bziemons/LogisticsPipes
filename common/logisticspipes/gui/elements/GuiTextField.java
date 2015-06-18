package logisticspipes.gui.elements;

import logisticspipes.gui.ILPGuiElement;
import net.minecraft.client.gui.FontRenderer;

public class GuiTextField extends net.minecraft.client.gui.GuiTextField implements ILPGuiElement {

	public GuiTextField(FontRenderer fontRenderer, int x, int y, int width, int height) {
		super(fontRenderer, x, y, width, height);
	}

	@Override public void draw() {
		super.drawTextBox();
	}

	@Override public boolean needsMouseInput() {
		return false;
	}

	@Override public void setMouseInput(int mouseX, int mouseY, int mouseButtonEvent) {
		throw new UnsupportedOperationException("\"needsMouseInput\" returns false");
	}
}
