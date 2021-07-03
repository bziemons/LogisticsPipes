package logisticspipes.interfaces;

import net.minecraft.util.Direction;

public interface IRotationProvider {

	@Deprecated
	int getRotation();

	default Direction getFacing() {
		switch (getRotation()) {
			case 0:
				return Direction.WEST;
			case 1:
				return Direction.EAST;
			case 2:
				return Direction.NORTH;
			case 3:
			default:
				return Direction.SOUTH;
		}
	}

	@Deprecated
	void setRotation(int rotation);

	default void setFacing(Direction facing) {
		switch (facing) {
			case NORTH:
				setRotation(3);
				break;
			case DOWN:
			case UP:
			case SOUTH:
				setRotation(2);
				break;
			case WEST:
				setRotation(1);
				break;
			case EAST:
				setRotation(0);
				break;
		}
	}

}
