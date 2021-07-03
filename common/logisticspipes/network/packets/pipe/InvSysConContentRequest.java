package logisticspipes.network.packets.pipe;

import net.minecraft.entity.player.PlayerEntity;

import logisticspipes.network.PacketHandler;
import network.rs485.logisticspipes.network.packets.CoordinatesPacket;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.pipes.PipeItemsInvSysConnector;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.proxy.MainProxy;
import logisticspipes.utils.StaticResolve;

@StaticResolve
public class InvSysConContentRequest extends CoordinatesPacket {

	public InvSysConContentRequest(int id) {
		super(id);
	}

	@Override
	public ModernPacket template() {
		return new InvSysConContentRequest(getId());
	}

	@Override
	public void processPacket(PlayerEntity player) {
		final LogisticsTileGenericPipe pipe = this.getPipe(player.world);
		if (pipe == null) {
			return;
		}
		if (pipe.pipe instanceof PipeItemsInvSysConnector) {
			MainProxy.sendPacketToPlayer(PacketHandler.getPacket(InvSysConContent.class).setIdentSet(((PipeItemsInvSysConnector) pipe.pipe).getExpectedItems()), player);
		}
	}
}
