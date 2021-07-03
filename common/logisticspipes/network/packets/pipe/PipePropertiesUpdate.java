package logisticspipes.network.packets.pipe;

import java.util.Objects;
import javax.annotation.Nonnull;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;

import logisticspipes.network.PacketHandler;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.proxy.MainProxy;
import network.rs485.logisticspipes.network.packets.CoordinatesPacket;
import network.rs485.logisticspipes.property.PropertyHolder;
import network.rs485.logisticspipes.util.LPDataInput;
import network.rs485.logisticspipes.util.LPDataOutput;

@StaticResolve
public class PipePropertiesUpdate extends CoordinatesPacket {

	@Nonnull
	public CompoundNBT tag = new CompoundNBT();

	public PipePropertiesUpdate(int id) {
		super(id);
	}

	@Override
	public void writeData(LPDataOutput output) {
		super.writeData(output);
		output.writeCompoundNBT(tag);
	}

	@Override
	public void readData(LPDataInput input) {
		super.readData(input);
		tag = Objects.requireNonNull(input.readCompoundNBT(), "read null NBT in PipePropertiesUpdate");
	}

	@Override
	public ModernPacket template() {
		return new PipePropertiesUpdate(getId());
	}

	@Override
	public void processPacket(PlayerEntity player) {
		LogisticsTileGenericPipe tile = this.getPipe(player.getEntityWorld(), LTGPCompletionCheck.PIPE);
		if (!(tile.pipe instanceof PropertyHolder)) {
			return;
		}

		// sync updated properties
		tile.pipe.readFromNBT(tag);

		MainProxy.runOnServer(player.world, () -> () -> {
			// resync client; always
			MainProxy.sendPacketToPlayer(fromPropertyHolder((PropertyHolder) tile.pipe).setPacketPos(this), player);
		});
	}

	@Nonnull
	public static PipePropertiesUpdate fromPropertyHolder(PropertyHolder holder) {
		final PipePropertiesUpdate packet = PacketHandler.getPacket(PipePropertiesUpdate.class);
		holder.writeToNBT(packet.tag);
		return packet;
	}

}
