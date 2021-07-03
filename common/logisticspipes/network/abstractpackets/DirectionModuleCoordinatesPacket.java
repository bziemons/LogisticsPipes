package logisticspipes.network.abstractpackets;

import net.minecraft.util.Direction;

import network.rs485.logisticspipes.network.packets.ModuleCoordinatesPacket;
import network.rs485.logisticspipes.util.LPDataInput;
import network.rs485.logisticspipes.util.LPDataOutput;

public abstract class DirectionModuleCoordinatesPacket extends ModuleCoordinatesPacket {
	private Direction direction;

	public DirectionModuleCoordinatesPacket(int id) {
		super(id);
	}

	public DirectionModuleCoordinatesPacket setDirection(Direction newDirection) {
		direction = newDirection;
		return this;
	}

	public Direction getDirection() {
		return direction;
	}

	@Override
	public void writeData(LPDataOutput output) {
		super.writeData(output);
		output.writeFacing(direction);
	}

	@Override
	public void readData(LPDataInput input) {
		super.readData(input);
		direction = input.readFacing();
	}
}
