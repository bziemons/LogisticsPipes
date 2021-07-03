package logisticspipes.asm.te;

import net.minecraft.util.Direction;

import network.rs485.logisticspipes.world.DoubleCoordinates;

public interface ITileEntityChangeListener {

	void pipeRemoved(DoubleCoordinates pos);

	void pipeAdded(DoubleCoordinates pos, Direction side);

	void pipeModified(DoubleCoordinates pos);
}
