package logisticspipes.network.packets.pipe;

import java.util.BitSet;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Direction;

import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import network.rs485.logisticspipes.network.packets.CoordinatesPacket;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.transport.PipeFluidTransportLogistics;
import logisticspipes.utils.StaticResolve;
import network.rs485.logisticspipes.util.LPDataInput;
import network.rs485.logisticspipes.util.LPDataOutput;

@StaticResolve
public class PipeFluidUpdate extends CoordinatesPacket {

	@Getter(value = AccessLevel.PRIVATE)
	@Setter
	private FluidStack[] renderCache = new FluidStack[Direction.values().length];
	private BitSet bits = new BitSet();

	public PipeFluidUpdate(int id) {
		super(id);
	}

	@Override
	public void readData(LPDataInput input) {
		super.readData(input);
		bits = input.readBitSet();
		for (int i = 0; i < renderCache.length; i++) {
			if (bits.get(i)) {
				renderCache[i] = new FluidStack(FluidRegistry.getFluid(input.readUTF()), input.readInt(), input.readCompoundNBT());
			}
		}
	}

	@Override
	public void writeData(LPDataOutput output) {
		super.writeData(output);
		for (int i = 0; i < renderCache.length; i++) {
			bits.set(i, renderCache[i] != null);
		}
		output.writeBitSet(bits);
		for (FluidStack aRenderCache : renderCache) {
			if (aRenderCache != null) {
				output.writeUTF(aRenderCache.getFluid().getName());
				output.writeInt(aRenderCache.amount);
				output.writeCompoundNBT(aRenderCache.tag);
			}
		}
	}

	@Override
	public void processPacket(PlayerEntity player) {
		LogisticsTileGenericPipe pipe = this.getPipe(player.world);
		if (pipe == null || pipe.pipe == null) {
			return;
		}
		if (!(pipe.pipe.transport instanceof PipeFluidTransportLogistics)) {
			return;
		}
		((PipeFluidTransportLogistics) pipe.pipe.transport).renderCache = renderCache;
	}

	@Override
	public ModernPacket template() {
		return new PipeFluidUpdate(getId());
	}

	@Override
	public boolean isCompressable() {
		return true;
	}
}
