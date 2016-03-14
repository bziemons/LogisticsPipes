package logisticspipes.gui;

import java.util.LinkedList;

public class GuiScreen {
	protected LinkedList<IActiveGuiElement> activeGuiElements;

	public GuiScreen() {
		activeGuiElements = new LinkedList<>();
	}

	public void draw() {
		for (IActiveGuiElement element : activeGuiElements) {
			element.draw();
		}
	}
}
