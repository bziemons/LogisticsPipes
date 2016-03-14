package logisticspipes.network.packets.module;

import network.rs485.logisticspipes.network.LPChannel;

import logisticspipes.modules.ModuleAdvancedExtractor;

import logisticspipes.network.abstractpackets.ModuleCoordinatesPacket;
import logisticspipes.network.packets.modules.AdvancedExtractorInclude;

import net.minecraft.entity.player.EntityPlayer;

import logisticspipes.utils.StaticResolve;

@StaticResolve
public class AdvancedExtractorIncludePacket extends ModuleCoordinatesPacket {

	public AdvancedExtractorIncludePacket(int id) {
		super(id);
	}

	@Override
	public AbstractPacket template() {
		return new AdvancedExtractorIncludePacket(getId());
	}

	@Override
	public void processPacket(EntityPlayer player) {
		final ModuleAdvancedExtractor module = this.getLogisticsModule(player, ModuleAdvancedExtractor.class);
		if (module == null) {
			return;
		}
		module.setItemsIncluded(!module.areItemsIncluded());
		LPChannel.sendPacketToPlayer(PacketHandler.getPacket(AdvancedExtractorInclude.class).setFlag(module.areItemsIncluded()).setPacketPos(this), player);
	}
}
