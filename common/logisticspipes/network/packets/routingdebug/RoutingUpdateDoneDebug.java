package logisticspipes.network.packets.routingdebug;

import net.minecraft.entity.player.PlayerEntity;

import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.routing.debug.ClientViewController;
import logisticspipes.utils.StaticResolve;
import network.rs485.logisticspipes.util.LPDataInput;
import network.rs485.logisticspipes.util.LPDataOutput;

@StaticResolve
public class RoutingUpdateDoneDebug extends ModernPacket {

	public RoutingUpdateDoneDebug(int id) {
		super(id);
	}

	@Override
	public void readData(LPDataInput input) {}

	@Override
	public void processPacket(PlayerEntity player) {
		ClientViewController.instance().done(this);
	}

	@Override
	public void writeData(LPDataOutput output) {}

	@Override
	public ModernPacket template() {
		return new RoutingUpdateDoneDebug(getId());
	}

	@Override
	public boolean isCompressable() {
		return true;
	}
}
