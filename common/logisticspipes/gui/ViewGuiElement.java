package logisticspipes.gui;

public class ViewGuiElement implements IActiveGuiElement {

	protected boolean visible;
	protected int xPos;
	protected int yPos;
	protected IGuiElement guiElement;

	public ViewGuiElement(IGuiElement guiElement) {
		this.guiElement = guiElement;
	}

	@Override public void draw() {
		guiElement.draw(xPos, yPos);
	}

	@Override public IActiveGuiElement getParent() {
		//TODO: ...
		return null;
	}

	@Override public boolean hasChildren() {
		//TODO: ...
		return false;
	}

	@Override public IActiveGuiElement[] getChildren() {
		//TODO: ...
		return new IActiveGuiElement[0];
	}

	@Override public int getX() {
		return xPos;
	}

	@Override public IActiveGuiElement setX(int xPos) {
		this.xPos = xPos;
		return this;
	}

	@Override public int getY() {
		return yPos;
	}

	@Override public IActiveGuiElement setY(int yPos) {
		this.yPos = yPos;
		return this;
	}

	@Override public boolean isVisible() {
		return visible;
	}

	@Override public IActiveGuiElement setVisible(boolean visible) {
		this.visible = visible;
		return this;
	}

	@Override public void updateTotalSize() {
		//TODO: ...
	}

	@Override public int getTotalWidth() {
		//TODO: ...
		return 0;
	}

	@Override public int getTotalHeight() {
		//TODO: ...
		return 0;
	}
}
