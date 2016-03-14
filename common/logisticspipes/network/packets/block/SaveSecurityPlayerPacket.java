package logisticspipes.network.packets.block;

import logisticspipes.blocks.LogisticsSecurityTileEntity;

import logisticspipes.network.abstractpackets.NBTCoordinatesPacket;

import net.minecraft.entity.player.EntityPlayer;

import logisticspipes.utils.StaticResolve;

@StaticResolve
public class SaveSecurityPlayerPacket extends NBTCoordinatesPacket {

	public SaveSecurityPlayerPacket(int id) {
		super(id);
	}

	@Override
	public AbstractPacket template() {
		return new SaveSecurityPlayerPacket(getId());
	}

	@Override
	public void processPacket(EntityPlayer player) {
		LogisticsSecurityTileEntity tile = this.getTile(player.world, LogisticsSecurityTileEntity.class);
		if (tile != null) {
			tile.saveNewSecuritySettings(getTag());
		}
	}
}
