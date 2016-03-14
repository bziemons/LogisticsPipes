package logisticspipes.gui;

public interface IGuiElement {

	void draw(int xPos, int yPos);

	boolean needsMouseInput();

	void setMouseInput(int mouseX, int mouseY, int mouseButtonEvent);

	boolean hasDynamicSize();

	int getWidth();

	int getHeight();


}
