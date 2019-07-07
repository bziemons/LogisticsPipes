package logisticspipes.network.abstractpackets;

import lombok.Getter;
import lombok.Setter;

import logisticspipes.network.packets.orderer.RequestSubmitPacket;
import logisticspipes.utils.item.ItemIdentifierStack;
import network.rs485.logisticspipes.util.LPDataInput;
import network.rs485.logisticspipes.util.LPDataOutput;

public abstract class RequestPacket extends CoordinatesPacket {

	@Getter
	@Setter
	private ItemIdentifierStack stack;

	@Getter
	@Setter
	private int dimension;

	public RequestPacket(int id) {
		super(id);
	}

	@Override
	public AbstractPacket template() {
		return new RequestSubmitPacket(getId());
	}

	@Override
	public void writeData(LPDataOutput output) {
		super.writeData(output);
		output.writeItemIdentifierStack(stack);
		output.writeInt(dimension);
	}

	@Override
	public void readData(LPDataInput input) {
		super.readData(input);
		stack = input.readItemIdentifierStack();
		dimension = input.readInt();
	}
}
