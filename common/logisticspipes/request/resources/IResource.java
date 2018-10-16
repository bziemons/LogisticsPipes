package logisticspipes.request.resources;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import logisticspipes.proxy.computers.interfaces.ILPCCTypeHolder;
import logisticspipes.utils.item.ItemIdentifier;
import logisticspipes.utils.item.ItemIdentifierStack;
import network.rs485.logisticspipes.util.LPDataOutput;
import network.rs485.logisticspipes.util.LPFinalSerializable;

/**
 * With Destination and amount
 */
public interface IResource extends ILPCCTypeHolder, LPFinalSerializable {

	ItemIdentifier getAsItem();

	int getRequestedAmount();

	boolean matches(ItemIdentifier itemType, MatchSettings settings);

	IResource clone(int multiplier);

	void writeData(LPDataOutput output);

	@SideOnly(Side.CLIENT)
	String getDisplayText(ColorCode missing);

	ItemIdentifierStack getDisplayItem();

	@Override
	default void write(LPDataOutput output) {
		ResourceNetwork.writeResource(output, this);
	}

	/**
	 * Settings only apply for the normal Item Implementation.
	 */
	enum MatchSettings {
		NORMAL,
		WITHOUT_NBT
	}

	enum ColorCode {
		NONE,
		MISSING,
		SUCCESS
	}
}
