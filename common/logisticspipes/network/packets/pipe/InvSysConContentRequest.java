package logisticspipes.network.packets.pipe;

import network.rs485.logisticspipes.network.LPChannel;

import logisticspipes.network.abstractpackets.CoordinatesPacket;

import logisticspipes.pipes.PipeItemsInvSysConnector;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;

import net.minecraft.entity.player.EntityPlayer;

import logisticspipes.utils.StaticResolve;

@StaticResolve
public class InvSysConContentRequest extends CoordinatesPacket {

	public InvSysConContentRequest(int id) {
		super(id);
	}

	@Override
	public AbstractPacket template() {
		return new InvSysConContentRequest(getId());
	}

	@Override
	public void processPacket(EntityPlayer player) {
		final LogisticsTileGenericPipe pipe = this.getPipe(player.world);
		if (pipe == null) {
			return;
		}
		if (pipe.pipe instanceof PipeItemsInvSysConnector) {
			LPChannel.sendPacketToPlayer(PacketHandler.getPacket(InvSysConContent.class).setIdentSet(((PipeItemsInvSysConnector) pipe.pipe).getExpectedItems()),
					player);
		}
	}
}
