package logisticspipes.network.packets.pipe;

import javax.annotation.Nullable;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Direction;

import lombok.Getter;
import lombok.Setter;

import network.rs485.logisticspipes.network.packets.CoordinatesPacket;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.pipes.PipeLogisticsChassis;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.utils.StaticResolve;

@StaticResolve
public class ChassisOrientationPacket extends CoordinatesPacket {

	@Getter
	@Setter
	@Nullable
	private Direction dir;

	public ChassisOrientationPacket(int id) {
		super(id);
	}

	@Override
	public void processPacket(PlayerEntity player) {
		LogisticsTileGenericPipe pipe = this.getPipe(player.world, LTGPCompletionCheck.PIPE);
		if (pipe.pipe instanceof PipeLogisticsChassis) {
			((PipeLogisticsChassis) pipe.pipe).setPointedOrientation(dir);
		}
	}

	@Override
	public ModernPacket template() {
		return new ChassisOrientationPacket(getId());
	}
}
