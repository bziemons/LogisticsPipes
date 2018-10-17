package logisticspipes.request.resources;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import lombok.Getter;

import logisticspipes.utils.item.ItemIdentifier;
import logisticspipes.utils.item.ItemIdentifierStack;
import logisticspipes.utils.string.ChatColor;
import network.rs485.logisticspipes.util.LPDataInput;
import network.rs485.logisticspipes.util.LPDataOutput;

public class ItemResource implements IResource {

	@Getter
	protected final ItemIdentifierStack stack;
	protected Object ccObject;

	public ItemResource(ItemIdentifierStack stack) {
		this.stack = stack;
	}

	public ItemResource(LPDataInput input) {
		stack = input.readItemIdentifierStack();
	}

	@Override
	public void writeData(LPDataOutput output) {
		output.writeItemIdentifierStack(stack);
	}

	@Override
	public int getAmount() {
		return stack.getStackSize();
	}

	public ItemIdentifier getItem() {
		return stack.getItem();
	}

	public ItemIdentifierStack getItemStack() {
		return stack;
	}

	@Override
	public boolean matches(ItemIdentifier itemType) {
		return stack.getItem().equals(itemType);
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
		builder.append(stack.getFriendlyName());
		if (code != ColorCode.NONE) {
			builder.append(ChatColor.WHITE);
		}
		return builder.toString();
	}

	@Override
	public ItemIdentifierStack getDisplayItem() {
		return stack;
	}
}
