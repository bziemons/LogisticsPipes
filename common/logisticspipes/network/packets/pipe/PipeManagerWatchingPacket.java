package logisticspipes.network.packets.pipe;

import net.minecraft.entity.player.PlayerEntity;

import lombok.Getter;
import lombok.Setter;

import network.rs485.logisticspipes.network.packets.CoordinatesPacket;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.utils.StaticResolve;
import network.rs485.logisticspipes.util.LPDataInput;
import network.rs485.logisticspipes.util.LPDataOutput;

@StaticResolve
public class PipeManagerWatchingPacket extends CoordinatesPacket {

	@Getter
	@Setter
	private boolean start;

	public PipeManagerWatchingPacket(int id) {
		super(id);
	}

	@Override
	public void processPacket(PlayerEntity player) {
		LogisticsTileGenericPipe pipe = this.getPipe(player.getEntityWorld());
		if (pipe == null || !(pipe.pipe instanceof CoreRoutedPipe)) {
			return;
		}
		CoreRoutedPipe cPipe = (CoreRoutedPipe) pipe.pipe;
		if (start) {
			cPipe.getOrderManager().startWatching(player);
		} else {
			cPipe.getOrderManager().stopWatching(player);
		}
	}

	@Override
	public ModernPacket template() {
		return new PipeManagerWatchingPacket(getId());
	}

	@Override
	public void writeData(LPDataOutput output) {
		super.writeData(output);
		output.writeBoolean(start);
	}

	@Override
	public void readData(LPDataInput input) {
		super.readData(input);
		start = input.readBoolean();
	}
}
