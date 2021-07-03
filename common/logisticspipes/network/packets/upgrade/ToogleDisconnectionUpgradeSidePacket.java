package logisticspipes.network.packets.upgrade;

import java.util.Objects;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.Direction;

import lombok.Getter;
import lombok.Setter;

import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.network.abstractpackets.SlotPacket;
import logisticspipes.pipes.upgrades.ConnectionUpgradeConfig;
import logisticspipes.utils.StaticResolve;
import logisticspipes.utils.gui.UpgradeSlot;
import network.rs485.logisticspipes.util.LPDataInput;
import network.rs485.logisticspipes.util.LPDataOutput;

@StaticResolve
public class ToogleDisconnectionUpgradeSidePacket extends SlotPacket {

	@Getter
	@Setter
	private Direction side;

	public ToogleDisconnectionUpgradeSidePacket(int id) {
		super(id);
	}

	@Override
	public void processPacket(PlayerEntity player) {
		UpgradeSlot slot = getSlot(player, UpgradeSlot.class);
		ItemStack stack = slot.getStack();
		if (stack.isEmpty()) return;

		if (!stack.hasTag()) {
			stack.setTag(new CompoundNBT());
		}

		CompoundNBT tag = Objects.requireNonNull(stack.getTag());
		String sideName = ConnectionUpgradeConfig.Sides.getNameForDirection(side);
		tag.putBoolean(sideName, !tag.getBoolean(sideName));

		stack.setTag(tag);

		slot.putStack(stack);
	}

	@Override
	public void writeData(LPDataOutput output) {
		super.writeData(output);
		output.writeFacing(side);
	}

	@Override
	public void readData(LPDataInput input) {
		super.readData(input);
		side = input.readFacing();
	}

	@Override
	public ModernPacket template() {
		return new ToogleDisconnectionUpgradeSidePacket(getId());
	}
}
