package logisticspipes.gui.elements;

import logisticspipes.gui.ILPGuiElement;
import net.minecraft.client.Minecraft;

public class GuiButton extends net.minecraft.client.gui.GuiButton implements ILPGuiElement {
	protected int mouseX;
	protected int mouseY;
	protected int mouseButtonEvent;

	public GuiButton(int id, int x, int y, String text) {
		super(id, x, y, text);
	}

	public GuiButton(int id, int x, int y, int width, int height, String text) {
		super(id, x, y, width, height, text);
	}

	@Override public void draw() {
		super.drawButton(Minecraft.getMinecraft(), mouseX, mouseY);
	}

	@Override public boolean needsMouseInput() {
		return true;
	}

	@Override public void setMouseInput(int mouseX, int mouseY, int mouseButtonEvent) {
		this.mouseX = mouseX;
		this.mouseY = mouseY;
		this.mouseButtonEvent = mouseButtonEvent;
	}
}
