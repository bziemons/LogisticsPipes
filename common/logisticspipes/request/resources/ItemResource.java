package logisticspipes.request.resources;

import logisticspipes.utils.item.ItemIdentifier;
import logisticspipes.utils.item.ItemIdentifierStack;
import logisticspipes.utils.string.ChatColor;
import network.rs485.logisticspipes.util.LPDataInput;
import network.rs485.logisticspipes.util.LPDataOutput;

public class ItemResource implements IResource {

	private final ItemIdentifierStack stack;
	private Object ccObject;

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
	public ItemIdentifier getAsItem() {
		return stack.getItem();
	}

	@Override
	public int getRequestedAmount() {
		return stack.getStackSize();
	}

	public ItemIdentifier getItem() {
		return stack.getItem();
	}

	public ItemIdentifierStack getItemStack() {
		return stack;
	}

	@Override
	public boolean matches(ItemIdentifier itemType, MatchSettings settings) {
		switch (settings) {
			case NORMAL:
				return stack.getItem().equals(itemType);
			case WITHOUT_NBT:
				return stack.getItem().equalsWithoutNBT(itemType);
		}
		return stack.getItem().equals(itemType);
	}

	@Override
	public IResource clone(int multiplier) {
		ItemIdentifierStack stack = this.stack.clone();
		stack.setStackSize(stack.getStackSize() * multiplier);
		return new ItemResource(stack);
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
