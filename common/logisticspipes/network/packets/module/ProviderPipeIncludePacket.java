package logisticspipes.network.packets.module;

import network.rs485.logisticspipes.network.LPChannel;

import logisticspipes.network.abstractpackets.CoordinatesPacket;

import logisticspipes.network.packets.modules.ProviderPipeInclude;
import logisticspipes.pipes.PipeItemsProviderLogistics;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;

import net.minecraft.entity.player.EntityPlayer;

import logisticspipes.utils.StaticResolve;

@StaticResolve
public class ProviderPipeIncludePacket extends CoordinatesPacket {

	public ProviderPipeIncludePacket(int id) {
		super(id);
	}

	@Override
	public AbstractPacket template() {
		return new ProviderPipeIncludePacket(getId());
	}

	@Override
	public void processPacket(EntityPlayer player) {
		final LogisticsTileGenericPipe pipe = this.getPipe(player.world);
		if (pipe == null) {
			return;
		}
		if (!(pipe.pipe instanceof PipeItemsProviderLogistics)) {
			return;
		}
		final PipeItemsProviderLogistics providerPipe = (PipeItemsProviderLogistics) pipe.pipe;
		providerPipe.setFilterExcluded(!providerPipe.isExcludeFilter());
		LPChannel.sendPacketToPlayer(
				PacketHandler.getPacket(ProviderPipeInclude.class).setInteger(providerPipe.isExcludeFilter() ? 1 : 0).setPosX(getPosX()).setPosY(getPosY())
						.setPosZ(getPosZ()), player);
	}
}
