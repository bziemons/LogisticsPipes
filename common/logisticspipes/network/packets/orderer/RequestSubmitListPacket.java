package logisticspipes.network.packets.orderer;

import net.minecraft.entity.player.EntityPlayer;

import logisticspipes.network.abstractpackets.InventoryModuleCoordinatesPacket;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.pipes.PipeBlockRequestTable;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.utils.StaticResolve;

@StaticResolve
public class RequestSubmitListPacket extends InventoryModuleCoordinatesPacket {

	public RequestSubmitListPacket(int id) {
		super(id);
	}

	@Override
	public ModernPacket template() {
		return new RequestSubmitListPacket(getId());
	}

	@Override
	public void processPacket(EntityPlayer player) {
		final LogisticsTileGenericPipe pipe = this.getPipe(player.world);
		if (pipe == null) {
			return;
		}
		if (!(pipe.pipe instanceof PipeBlockRequestTable)) {
			return;
		}
		// TODO PROVIDE REFACTOR: request list
		//RequestHandler.requestList(player, getIdentList(), (CoreRoutedPipe) pipe.pipe);
	}
}
