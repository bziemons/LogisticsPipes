package logisticspipes.request.resources;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import logisticspipes.proxy.computers.interfaces.ILPCCTypeHolder;
import logisticspipes.utils.item.ItemIdentifier;
import logisticspipes.utils.item.ItemIdentifierStack;
import network.rs485.logisticspipes.util.LPDataOutput;
import network.rs485.logisticspipes.util.LPFinalSerializable;

public interface IResource extends ILPCCTypeHolder, LPFinalSerializable {

	int getAmount();

	boolean matches(ItemIdentifier itemType);

	void writeData(LPDataOutput output);

	@SideOnly(Side.CLIENT)
	String getDisplayText(ColorCode missing);

	ItemIdentifierStack getDisplayItem();

	@Override
	default void write(LPDataOutput output) {
		ResourceNetwork.writeResource(output, this);
	}

	enum ColorCode {
		NONE,
		MISSING,
		SUCCESS
	}
}
