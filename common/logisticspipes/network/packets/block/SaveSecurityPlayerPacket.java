package logisticspipes.network.packets.block;

import net.minecraft.entity.player.PlayerEntity;

import logisticspipes.blocks.LogisticsSecurityTileEntity;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.network.abstractpackets.NBTCoordinatesPacket;
import logisticspipes.utils.StaticResolve;

@StaticResolve
public class SaveSecurityPlayerPacket extends NBTCoordinatesPacket {

	public SaveSecurityPlayerPacket(int id) {
		super(id);
	}

	@Override
	public ModernPacket template() {
		return new SaveSecurityPlayerPacket(getId());
	}

	@Override
	public void processPacket(PlayerEntity player) {
		LogisticsSecurityTileEntity tile = this.getTileAs(player.world, LogisticsSecurityTileEntity.class);
		if (tile != null) {
			tile.saveNewSecuritySettings(getTag());
		}
	}
}
