package logisticspipes.network.packets.pipe;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.text.StringTextComponent;

import network.rs485.logisticspipes.network.packets.CoordinatesPacket;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.utils.StaticResolve;

@StaticResolve
public class PipeDebugResponse extends CoordinatesPacket {

	public PipeDebugResponse(int id) {
		super(id);
	}

	@Override
	public void processPacket(PlayerEntity player) {
		LogisticsTileGenericPipe pipe = this.getPipe(player.getEntityWorld());
		if (pipe != null && pipe.isInitialized()) {
			pipe.pipe.debug.debugThisPipe = !pipe.pipe.debug.debugThisPipe;
			if (pipe.pipe.debug.debugThisPipe) {
				player.sendMessage(new StringTextComponent("Debug enabled on Server"));
			} else {
				player.sendMessage(new StringTextComponent("Debug disabled on Server"));
			}
		}
	}

	@Override
	public ModernPacket template() {
		return new PipeDebugResponse(getId());
	}
}
