package logisticspipes.network.packets.debug;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.text.StringTextComponent;

import network.rs485.logisticspipes.network.packets.CoordinatesPacket;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.utils.StaticResolve;

@StaticResolve
public class PipeDebugLogResponse extends CoordinatesPacket {

	public PipeDebugLogResponse(int id) {
		super(id);
	}

	@Override
	public void processPacket(PlayerEntity player) {
		LogisticsTileGenericPipe tile = this.getPipe(player.getEntityWorld());
		if (tile != null) {
			((CoreRoutedPipe) tile.pipe).debug.openForPlayer(player);
			player.sendMessage(new StringTextComponent("Debug log enabled."));
		}
	}

	@Override
	public ModernPacket template() {
		return new PipeDebugLogResponse(getId());
	}

	@Override
	public boolean isCompressable() {
		return true;
	}
}
