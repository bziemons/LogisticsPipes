package logisticspipes.network.packets.block;

import net.minecraft.entity.player.PlayerEntity;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.client.FMLClientHandler;

import logisticspipes.gui.GuiSecurityStation;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.network.abstractpackets.NBTCoordinatesPacket;
import logisticspipes.proxy.MainProxy;
import logisticspipes.security.SecuritySettings;
import logisticspipes.utils.StaticResolve;

@StaticResolve
public class SecurityStationOpenPlayer extends NBTCoordinatesPacket {

	public SecurityStationOpenPlayer(int id) {
		super(id);
	}

	@Override
	public ModernPacket template() {
		return new SecurityStationOpenPlayer(getId());
	}

	@Override
	public void processPacket(PlayerEntity player) {
		if (MainProxy.isClient(player.world)) {
			handleClientSide(player);
		} else {

		}
	}

	@OnlyIn(Dist.CLIENT)
	private void handleClientSide(PlayerEntity player) {
		if (FMLClientHandler.instance().getClient().currentScreen instanceof GuiSecurityStation) {
			SecuritySettings setting = new SecuritySettings(null);
			setting.readFromNBT(getTag());
			((GuiSecurityStation) FMLClientHandler.instance().getClient().currentScreen).handlePlayerSecurityOpen(setting);
		}
	}
}
