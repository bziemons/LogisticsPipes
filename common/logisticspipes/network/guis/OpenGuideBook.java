package logisticspipes.network.guis;

import javax.annotation.Nonnull;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;

import logisticspipes.network.abstractpackets.ModernPacket;
import network.rs485.logisticspipes.guidebook.ItemGuideBook;
import network.rs485.logisticspipes.util.LPDataInput;
import network.rs485.logisticspipes.util.LPDataOutput;

public class OpenGuideBook extends ModernPacket {

	private Hand hand;
	private ItemStack stack;

	public OpenGuideBook(int id) {
		super(id);
	}

	@Override
	public void readData(LPDataInput input) {
		super.readData(input);
		hand = input.readEnum(Hand.class);
		stack = input.readItemStack();
	}

	@Override
	public void writeData(LPDataOutput output) {
		super.writeData(output);
		output.writeEnum(hand);
		output.writeItemStack(stack);
	}

	@Override
	public void processPacket(PlayerEntity player) {
		ItemGuideBook.openGuideBook(hand, stack);
	}

	@Override
	public ModernPacket template() {
		return new OpenGuideBook(getId());
	}

	@Nonnull
	public OpenGuideBook setHand(@Nonnull Hand hand) {
		this.hand = hand;
		return this;
	}

	@Nonnull
	public OpenGuideBook setStack(@Nonnull ItemStack stack) {
		this.stack = stack;
		return this;
	}
}
