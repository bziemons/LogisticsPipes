package logisticspipes.gui;

public class ActiveGuiElementFactory {

	private static final ActiveGuiElementFactory instance = new ActiveGuiElementFactory();

	public ActiveGuiElementFactory getInstance() {
		return instance;
	}

	public IActiveGuiElement create() {
		return new IActiveGuiElement() {

			@Override public void draw() {

			}

			@Override public IActiveGuiElement getParent() {
				return null;
			}

			@Override public boolean hasChildren() {
				return false;
			}

			@Override public IActiveGuiElement[] getChildren() {
				return new IActiveGuiElement[0];
			}

			@Override public int getX() {
				return 0;
			}

			@Override public IActiveGuiElement setX(int xPos) {
				return null;
			}

			@Override public int getY() {
				return 0;
			}

			@Override public IActiveGuiElement setY(int yPos) {
				return null;
			}

			@Override public boolean isVisible() {
				return false;
			}

			@Override public IActiveGuiElement setVisible(boolean visible) {
				return null;
			}

			@Override public void updateTotalSize() {

			}

			@Override public int getTotalWidth() {
				return 0;
			}

			@Override public int getTotalHeight() {
				return 0;
			}
		};
	}
}
