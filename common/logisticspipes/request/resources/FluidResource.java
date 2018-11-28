package logisticspipes.request.resources;

import logisticspipes.utils.FluidIdentifier;
import logisticspipes.utils.item.ItemIdentifier;
import network.rs485.logisticspipes.util.LPDataInput;
import network.rs485.logisticspipes.util.LPDataOutput;

public class FluidResource implements IResource {

	private final FluidIdentifier liquid;
	private int amount;
	private Object ccObject;

	public FluidResource(FluidIdentifier liquid, int amount) {
		this.liquid = liquid;
		this.amount = amount;
	}

	public FluidResource(LPDataInput input) {
		liquid = FluidIdentifier.get(input.readItemIdentifier());
		amount = input.readInt();
	}

	@Override
	public void writeData(LPDataOutput output) {
		output.writeItemIdentifier(liquid.getItemIdentifier());
		output.writeInt(amount);
	}

	public FluidIdentifier getFluid() {
		return liquid;
	}

	public boolean matches(ItemIdentifier itemType) {
		if (itemType.isFluidContainer()) {
			FluidIdentifier other = FluidIdentifier.get(itemType);
			return other.equals(liquid);
		}
		return false;
	}

	@Override
	public Object getCCType() {
		return ccObject;
	}

	@Override
	public void setCCType(Object type) {
		ccObject = type;
	}

}
