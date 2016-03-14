package logisticspipes.gui;

public interface IActiveGuiElement {

	void draw();

	IActiveGuiElement getParent();

	boolean hasChildren();

	IActiveGuiElement[] getChildren();

	int getX();

	IActiveGuiElement setX(int xPos);

	int getY();

	IActiveGuiElement setY(int yPos);

	boolean isVisible();

	IActiveGuiElement setVisible(boolean visible);

	void updateTotalSize();

	int getTotalWidth();

	int getTotalHeight();
}
