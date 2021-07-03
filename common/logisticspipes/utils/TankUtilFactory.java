package logisticspipes.utils;

import javax.annotation.Nullable;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;

import logisticspipes.LogisticsPipes;
import logisticspipes.interfaces.ITankUtil;

public class TankUtilFactory {

	@Nullable
	public ITankUtil getTankUtilForTE(TileEntity tile, Direction direction) {
		if (tile == null) return null;
		// TODO: Propagate Optional
		//noinspection ConstantConditions
		return tile.getCapability(LogisticsPipes.FLUID_HANDLER_CAPABILITY, direction).map(TankUtil::new).orElse(null);
	}
}
