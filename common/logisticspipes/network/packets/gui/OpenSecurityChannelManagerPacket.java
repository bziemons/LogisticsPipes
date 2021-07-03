package logisticspipes.network.packets.gui;

import net.minecraft.entity.player.PlayerEntity;

import logisticspipes.blocks.LogisticsSecurityTileEntity;
import logisticspipes.interfaces.routing.IChannelManager;
import logisticspipes.network.NewGuiHandler;
import network.rs485.logisticspipes.network.packets.CoordinatesPacket;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.network.guis.block.SecurityChannelManagerGui;
import logisticspipes.proxy.SimpleServiceLocator;
import logisticspipes.utils.StaticResolve;

@StaticResolve
public class OpenSecurityChannelManagerPacket extends CoordinatesPacket {

	public OpenSecurityChannelManagerPacket(int id) {
		super(id);
	}

	@Override
	public void processPacket(PlayerEntity player) {
		LogisticsSecurityTileEntity securityTile = this.getTileAs(player.getEntityWorld(), LogisticsSecurityTileEntity.class);
		IChannelManager manager = SimpleServiceLocator.channelManagerProvider.getChannelManager(player.getEntityWorld());
		NewGuiHandler.getGui(SecurityChannelManagerGui.class).setChannelInformations(manager.getAllowedChannels(player)).setTilePos(securityTile).open(player);
	}

	@Override
	public ModernPacket template() {
		return new OpenSecurityChannelManagerPacket(getId());
	}
}
