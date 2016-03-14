package logisticspipes.network.packets.cpipe;

import network.rs485.logisticspipes.network.LPChannel;

import logisticspipes.modules.ModuleCrafter;

import logisticspipes.network.abstractpackets.ModuleCoordinatesPacket;

import net.minecraft.entity.player.EntityPlayer;

import logisticspipes.utils.StaticResolve;

@StaticResolve
public class CPipeCleanupImport extends ModuleCoordinatesPacket {

	public CPipeCleanupImport(int id) {
		super(id);
	}

	@Override
	public AbstractPacket template() {
		return new CPipeCleanupImport(getId());
	}

	@Override
	public void processPacket(EntityPlayer player) {
		final ModuleCrafter module = this.getLogisticsModule(player, ModuleCrafter.class);
		if (module == null) {
			return;
		}
		module.importCleanup();
		LPChannel.sendPacketToPlayer(PacketHandler.getPacket(CPipeCleanupStatus.class).setMode(module.cleanupModeIsExclude).setPacketPos(this), player);
	}
}
