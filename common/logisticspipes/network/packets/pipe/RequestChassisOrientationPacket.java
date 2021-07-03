package logisticspipes.network.packets.pipe;

import logisticspipes.network.PacketHandler;
import network.rs485.logisticspipes.network.packets.CoordinatesPacket;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.pipes.PipeLogisticsChassis;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.proxy.MainProxy;
import logisticspipes.utils.StaticResolve;

@StaticResolve
public class RequestChassisOrientationPacket extends CoordinatesPacket {

	public RequestChassisOrientationPacket(int id) {
		super(id);
	}

	@Override
	public void processPacket(PlayerEntity player) {
		LogisticsTileGenericPipe pipe = this.getPipe(player.world, LTGPCompletionCheck.PIPE);
		if (pipe.pipe instanceof PipeLogisticsChassis) {
			MainProxy.sendPacketToPlayer(PacketHandler.getPacket(ChassisOrientationPacket.class).setDir(((PipeLogisticsChassis) pipe.pipe).getPointedDirection()).setPosX(getPosX()).setPosY(getPosY()).setPosZ(getPosZ()), player);
		}
	}

	@Override
	public ModernPacket template() {
		return new RequestChassisOrientationPacket(getId());
	}
}
