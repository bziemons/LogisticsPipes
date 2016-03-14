package logisticspipes.network.packets.block;

import logisticspipes.interfaces.IRotationProvider;
import logisticspipes.network.abstractpackets.IntegerCoordinatesPacket;


import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;

import logisticspipes.utils.StaticResolve;

@StaticResolve
public class Rotation extends IntegerCoordinatesPacket {

	public Rotation(int id) {
		super(id);
	}

	@Override
	public AbstractPacket template() {
		return new Rotation(getId());
	}

	@Override
	public void processPacket(EntityPlayer player) {
		IRotationProvider tile = this.getTileOrPipe(player.world, IRotationProvider.class);
		if (tile != null) {
			tile.setRotation(getInteger());
			player.world.notifyNeighborsRespectDebug(new BlockPos(getPosX(), getPosY(), getPosZ()), player.world.getBlockState(new BlockPos(getPosX(), getPosY(), getPosZ())).getBlock(), true);
		}
	}
}
