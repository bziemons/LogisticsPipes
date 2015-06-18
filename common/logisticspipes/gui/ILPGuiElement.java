package logisticspipes.gui;

public interface ILPGuiElement {
	void draw();

	boolean needsMouseInput();

	void setMouseInput(int mouseX, int mouseY, int mouseButtonEvent);
}
