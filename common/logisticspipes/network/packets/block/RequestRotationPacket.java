package logisticspipes.network.packets.block;

import network.rs485.logisticspipes.network.LPChannel;

import logisticspipes.interfaces.IRotationProvider;
import logisticspipes.network.abstractpackets.CoordinatesPacket;


import net.minecraft.entity.player.EntityPlayer;

import logisticspipes.utils.StaticResolve;

@StaticResolve
public class RequestRotationPacket extends CoordinatesPacket {

	public RequestRotationPacket(int id) {
		super(id);
	}

	@Override
	public AbstractPacket template() {
		return new RequestRotationPacket(getId());
	}

	@Override
	public void processPacket(EntityPlayer player) {
		IRotationProvider tile = this.getTileOrPipe(player.world, IRotationProvider.class);
		if (tile != null) {
			LPChannel.sendPacketToPlayer(
					PacketHandler.getPacket(Rotation.class).setInteger(tile.getRotation()).setPosX(getPosX()).setPosY(getPosY()).setPosZ(getPosZ()), player);
		}
	}
}
