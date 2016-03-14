package logisticspipes.network.packets.module;

import network.rs485.logisticspipes.network.LPChannel;

import logisticspipes.network.abstractpackets.CoordinatesPacket;

import logisticspipes.network.packets.modules.ProviderPipeMode;
import logisticspipes.pipes.PipeItemsProviderLogistics;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;

import net.minecraft.entity.player.EntityPlayer;

import logisticspipes.utils.StaticResolve;

@StaticResolve
public class ProviderPipeNextModePacket extends CoordinatesPacket {

	public ProviderPipeNextModePacket(int id) {
		super(id);
	}

	@Override
	public AbstractPacket template() {
		return new ProviderPipeNextModePacket(getId());
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
		final PipeItemsProviderLogistics providerpipe = (PipeItemsProviderLogistics) pipe.pipe;
		providerpipe.nextExtractionMode();
		LPChannel.sendPacketToPlayer(
				PacketHandler.getPacket(ProviderPipeMode.class).setInteger(providerpipe.getExtractionMode().ordinal()).setPosX(getPosX()).setPosY(getPosY())
						.setPosZ(getPosZ()), player);
	}
}
