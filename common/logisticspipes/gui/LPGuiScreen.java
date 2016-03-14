package logisticspipes.gui;

import net.minecraft.client.Minecraft;

public class LPGuiScreen extends net.minecraft.client.gui.GuiScreen {

	private Gui gui;

	public LPGuiScreen(Gui gui) {
		// null all unnecessary GuiScreen attributes
		itemRender = null;
		buttonList = null;
		labelList = null;

		this.gui = gui;
	}

	@Override public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		gui.onMouseOver(mouseX, mouseY);
		gui.draw();
	}

	@Override public void setWorldAndResolution(Minecraft mc, int width, int height) {
		this.mc = mc;
		this.fontRendererObj = mc.fontRenderer;
		this.width = width;
		this.height = height;
	}

	@Override protected void mouseClicked(int mouseX, int mouseY, int eventButton) {

	}

	@Override protected void mouseClickMove(int mouseX, int mouseY, int lastButtonClicked, long timeSinceMouseClick) {

	}

	@Override protected void mouseMovedOrUp(int mouseX, int mouseY, int eventButton) {

	}
}
