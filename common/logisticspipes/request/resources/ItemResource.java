package logisticspipes.request.resources;

import lombok.Getter;

import logisticspipes.utils.item.ItemIdentifier;
import logisticspipes.utils.item.ItemIdentifierStack;
import network.rs485.logisticspipes.util.LPDataInput;
import network.rs485.logisticspipes.util.LPDataOutput;

public class ItemResource implements IResource {

	private final Object[] ccTypeHolder = new Object[1];
	private final IRequestItems requester;

	@Getter
	protected final ItemIdentifierStack stack;

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

	public ItemIdentifier getItem() {
		return stack.getItem();
	}

	public ItemIdentifierStack getItemStack() {
		return stack;
	}

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

}
