package logisticspipes.request.resources;

import logisticspipes.proxy.computers.interfaces.ILPCCTypeHolder;
import network.rs485.logisticspipes.util.LPDataOutput;
import network.rs485.logisticspipes.util.LPFinalSerializable;

public interface IResource extends ILPCCTypeHolder, LPFinalSerializable {

	void writeData(LPDataOutput output);

	@Override
	default void write(LPDataOutput output) {
		ResourceNetwork.writeResource(output, this);
	}
}
