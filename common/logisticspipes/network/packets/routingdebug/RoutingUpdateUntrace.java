package logisticspipes.network.packets.routingdebug;

import logisticspipes.network.abstractpackets.IntegerPacket;

import logisticspipes.routing.debug.DebugController;

import net.minecraft.entity.player.EntityPlayer;

import logisticspipes.utils.StaticResolve;

@StaticResolve
public class RoutingUpdateUntrace extends IntegerPacket {

	public RoutingUpdateUntrace(int id) {
		super(id);
	}

	@Override
	public void processPacket(EntityPlayer player) {
		DebugController.instance(player).untrace(getInteger());
	}

	@Override
	public AbstractPacket template() {
		return new RoutingUpdateUntrace(getId());
	}
}
