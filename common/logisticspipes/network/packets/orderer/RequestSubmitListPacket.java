package logisticspipes.network.packets.orderer;

import net.minecraft.entity.player.PlayerEntity;

import logisticspipes.network.abstractpackets.InventoryModuleCoordinatesPacket;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.pipes.PipeBlockRequestTable;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.request.RequestHandler;
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
	public void processPacket(PlayerEntity player) {
		final LogisticsTileGenericPipe pipe = this.getPipe(player.world);
		if (pipe == null) {
			return;
		}
		if (!(pipe.pipe instanceof PipeBlockRequestTable)) {
			return;
		}
		RequestHandler.requestList(player, getIdentList(), (CoreRoutedPipe) pipe.pipe);
	}
}
