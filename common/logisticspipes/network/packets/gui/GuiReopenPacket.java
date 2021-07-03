package logisticspipes.network.packets.gui;

import net.minecraft.entity.player.PlayerEntity;

import lombok.Getter;
import lombok.Setter;

import logisticspipes.LogisticsPipes;
import network.rs485.logisticspipes.network.packets.CoordinatesPacket;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.utils.StaticResolve;
import network.rs485.logisticspipes.util.LPDataInput;
import network.rs485.logisticspipes.util.LPDataOutput;

@StaticResolve
public class GuiReopenPacket extends CoordinatesPacket {

	@Getter
	@Setter
	private int guiID;

	public GuiReopenPacket(int id) {
		super(id);
	}

	@Override
	public void writeData(LPDataOutput output) {
		super.writeData(output);
		output.writeInt(getGuiID());
	}

	@Override
	public void readData(LPDataInput input) {
		super.readData(input);
		guiID = input.readInt();
	}

	@Override
	public void processPacket(PlayerEntity player) {
		player.openGui(LogisticsPipes.instance, getGuiID(), player.world, getPosX(), getPosY(), getPosZ());
	}

	@Override
	public ModernPacket template() {
		return new GuiReopenPacket(getId());
	}

}
