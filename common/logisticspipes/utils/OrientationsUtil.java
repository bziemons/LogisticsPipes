package logisticspipes.utils;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;

public class OrientationsUtil {

	public static Direction getOrientationOfTilewithTile(TileEntity pipeTile, TileEntity tileTile) {
		final BlockPos pipe = pipeTile.getPos();
		final BlockPos other = tileTile.getPos();
		if (pipe.getZ() == other.getZ()) {
			if (pipe.getY() == other.getY()) {
				if (pipe.getX() < other.getX()) {
					return Direction.EAST;
				} else if (pipe.getX() > other.getX()) {
					return Direction.WEST;
				}
			}
		}
		if (pipe.getX() == other.getX()) {
			if (pipe.getZ() == other.getZ()) {
				if (pipe.getY() < other.getY()) {
					return Direction.UP;
				} else if (pipe.getY() > other.getY()) {
					return Direction.DOWN;
				}
			}
		}
		if (pipe.getX() == other.getX()) {
			if (pipe.getY() == other.getY()) {
				if (pipe.getZ() < other.getZ()) {
					return Direction.SOUTH;
				} else if (pipe.getZ() > other.getZ()) {
					return Direction.NORTH;
				}
			}
		}
		return null;
	}

}
