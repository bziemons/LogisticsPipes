package logisticspipes.network.packets.gui;

import java.util.UUID;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;

import logisticspipes.blocks.LogisticsSecurityTileEntity;
import logisticspipes.network.NewGuiHandler;
import network.rs485.logisticspipes.network.packets.CoordinatesPacket;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.network.guis.AddChannelGuiProvider;
import logisticspipes.utils.StaticResolve;

@StaticResolve
public class OpenAddChannelGUIPacket extends CoordinatesPacket {

	public OpenAddChannelGUIPacket(int id) {
		super(id);
	}

	@Override
	public void processPacket(PlayerEntity player) {
		TileEntity tile = player.getEntityWorld().getTileEntity(new BlockPos(getPosX(), getPosY(), getPosZ()));
		UUID securityID = null;
		if (tile instanceof LogisticsSecurityTileEntity) {
			LogisticsSecurityTileEntity security = (LogisticsSecurityTileEntity) tile;
			securityID = security.getSecId();
		}
		UUID finalSecurityID = securityID;
		NewGuiHandler.getGui(AddChannelGuiProvider.class).setSecurityStationID(finalSecurityID).open(player);
	}

	@Override
	public ModernPacket template() {
		return new OpenAddChannelGUIPacket(getId());
	}
}
