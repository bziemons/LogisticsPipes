package logisticspipes.network.packets.pipe;

import net.minecraft.entity.player.PlayerEntity;

import logisticspipes.interfaces.ISendQueueContentRecieiver;
import logisticspipes.network.abstractpackets.InventoryModuleCoordinatesPacket;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.utils.StaticResolve;

@StaticResolve
public class SendQueueContent extends InventoryModuleCoordinatesPacket {

	public SendQueueContent(int id) {
		super(id);
	}

	@Override
	public ModernPacket template() {
		return new SendQueueContent(getId());
	}

	@Override
	public void processPacket(PlayerEntity player) {
		final LogisticsTileGenericPipe pipe = this.getPipe(player.world);
		if (pipe == null) {
			return;
		}
		if (pipe.pipe instanceof ISendQueueContentRecieiver) {
			ISendQueueContentRecieiver receiver = (ISendQueueContentRecieiver) pipe.pipe;
			receiver.handleSendQueueItemIdentifierList(getIdentList());
		}
	}

	@Override
	public boolean isCompressable() {
		return true;
	}
}
