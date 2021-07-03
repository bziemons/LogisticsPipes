package logisticspipes.utils;

import net.minecraft.util.Direction;

public class DirectionUtil {

	public static Direction getOrientation(int input) {
		if (input < 0 || Direction.values().length <= input) {
			return null;
		}
		return Direction.values()[input];
	}
}
