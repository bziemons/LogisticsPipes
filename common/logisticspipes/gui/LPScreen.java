package logisticspipes.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;

public class LPScreen extends GuiScreen {

	public LPScreen() {
		// null all unnecessary GuiScreen attributes
		itemRender = null;
		buttonList = null;
		labelList = null;
	}

	@Override public void drawScreen(int mouseX, int mouseY, float partialTicks) {

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
