package logisticspipes.gui;

public class Gui {
	public GuiSession guiSession;
	public GuiScreen guiScreen;
	public GuiScreen activeGuiScreen;

	public void onKey() {

	}

	public void onClick() {

	}

	public void onMouseOver(int mouseX, int mouseY) {

	}

	public void closeScreen() {

	}

	public void draw() {
		activeGuiScreen.draw();
	}
}
