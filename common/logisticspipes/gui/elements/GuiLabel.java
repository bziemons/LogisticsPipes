package logisticspipes.gui.elements;

import logisticspipes.gui.IGuiElement;

import net.minecraft.client.Minecraft;

public class GuiLabel extends net.minecraft.client.gui.GuiLabel implements IGuiElement {

	@Override public void draw(int xPos, int yPos) {
		// TODO: Temp var
		Minecraft mc = Minecraft.getMinecraft();
		// TODO: x, y ?
		func_146159_a(mc, 0, 0);
	}

	@Override public boolean needsMouseInput() {
		return false;
	}

	@Override public void setMouseInput(int mouseX, int mouseY, int mouseButtonEvent) {}

	@Override public boolean hasDynamicSize() {
		return false;
	}

	@Override public int getWidth() {
		return 0;
	}

	@Override public int getHeight() {
		return 0;
	}
}
