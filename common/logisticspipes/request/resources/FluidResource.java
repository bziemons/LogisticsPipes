package logisticspipes.request.resources;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import logisticspipes.utils.FluidIdentifier;
import logisticspipes.utils.item.ItemIdentifier;
import logisticspipes.utils.item.ItemIdentifierStack;
import logisticspipes.utils.string.ChatColor;
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

	@Override
	public int getAmount() {
		return amount;
	}

	public FluidIdentifier getFluid() {
		return liquid;
	}

	@Override
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

	@Override
	@SideOnly(Side.CLIENT)
	public String getDisplayText(ColorCode code) {
		StringBuilder builder = new StringBuilder();
		if (code != ColorCode.NONE) {
			builder.append(code == ColorCode.MISSING ? ChatColor.RED : ChatColor.GREEN);
		}
		builder.append(amount);
		builder.append("mB ");
		builder.append(liquid.makeFluidStack(0).getLocalizedName());
		if (code != ColorCode.NONE) {
			builder.append(ChatColor.WHITE);
		}
		return builder.toString();
	}

	@Override
	public ItemIdentifierStack getDisplayItem() {
		return liquid.getItemIdentifier().makeStack(amount);
	}
}
