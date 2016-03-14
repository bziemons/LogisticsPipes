package logisticspipes.network.packets.module;

import network.rs485.logisticspipes.network.LPChannel;

import logisticspipes.modules.ModuleCrafter;

import logisticspipes.network.abstractpackets.ModuleCoordinatesPacket;
import logisticspipes.network.packets.cpipe.CPipeSatelliteImportBack;

import net.minecraft.entity.player.EntityPlayer;

public class RequestCraftingPipeUpdatePacket extends ModuleCoordinatesPacket {

	public RequestCraftingPipeUpdatePacket(int id) {
		super(id);
	}

	@Override
	public void processPacket(EntityPlayer player) {
		ModuleCrafter module = this.getLogisticsModule(player, ModuleCrafter.class);
		if (module == null) {
			return;
		}


		LPChannel.sendPacketToPlayer(module.getCPipePacket(), player);
		LPChannel.sendPacketToPlayer(PacketHandler.getPacket(CPipeSatelliteImportBack.class).setInventory(module.getDummyInventory()).setModulePos(module),
				player);
	}
}
