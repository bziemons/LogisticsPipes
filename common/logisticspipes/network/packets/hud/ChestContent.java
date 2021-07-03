package logisticspipes.network.packets.hud;

import net.minecraft.entity.player.PlayerEntity;

import logisticspipes.interfaces.IChestContentReceiver;
import logisticspipes.network.abstractpackets.InventoryModuleCoordinatesPacket;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.utils.StaticResolve;

@StaticResolve
public class ChestContent extends InventoryModuleCoordinatesPacket {

	public ChestContent(int id) {
		super(id);
	}

	@Override
	public ModernPacket template() {
		return new ChestContent(getId());
	}

	@Override
	public void processPacket(PlayerEntity player) {
		final LogisticsTileGenericPipe pipe = this.getPipe(player.world);
		if (pipe != null && pipe.pipe instanceof IChestContentReceiver) {
			((IChestContentReceiver) pipe.pipe).setReceivedChestContent(getIdentList());
		}

	}

	@Override
	public boolean isCompressable() {
		return true;
	}
}
