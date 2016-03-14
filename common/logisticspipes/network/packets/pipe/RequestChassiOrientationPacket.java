package logisticspipes.network.packets.pipe;

import network.rs485.logisticspipes.network.LPChannel;

import logisticspipes.network.abstractpackets.CoordinatesPacket;

import logisticspipes.pipes.PipeLogisticsChassi;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;

import net.minecraft.entity.player.EntityPlayer;

import logisticspipes.utils.StaticResolve;

@StaticResolve
public class RequestChassiOrientationPacket extends CoordinatesPacket {

	public RequestChassiOrientationPacket(int id) {
		super(id);
	}

	@Override
	public void processPacket(EntityPlayer player) {
		LogisticsTileGenericPipe pipe = this.getPipe(player.world);
		if (pipe == null || !(pipe.pipe instanceof PipeLogisticsChassi)) {
			return;
		}
		LPChannel.sendPacketToPlayer(
				PacketHandler.getPacket(ChassiOrientationPacket.class).setDir(((PipeLogisticsChassi) pipe.pipe).getPointedOrientation()).setPosX(getPosX())
						.setPosY(getPosY()).setPosZ(getPosZ()), player);
	}

	@Override
	public AbstractPacket template() {
		return new RequestChassiOrientationPacket(getId());
	}
}
