package logisticspipes.pipes;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;

import net.minecraft.item.Item;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.Direction;

import net.minecraftforge.fluids.FluidStack;

import logisticspipes.logisticspipes.IRoutedItem;
import logisticspipes.logisticspipes.IRoutedItem.TransportMode;
import logisticspipes.pipes.basic.fluid.FluidRoutedPipe;
import logisticspipes.proxy.SimpleServiceLocator;
import logisticspipes.textures.Textures;
import logisticspipes.textures.Textures.TextureType;
import logisticspipes.transport.PipeFluidTransportLogistics;
import logisticspipes.utils.FluidIdentifierStack;
import logisticspipes.utils.FluidSinkReply;
import logisticspipes.utils.item.ItemIdentifierStack;
import logisticspipes.utils.tuples.Pair;

public class PipeFluidInsertion extends FluidRoutedPipe {

	private final List<Pair<Integer, Integer>> localJamList = new ArrayList<>();
	private int[] nextSendMax = new int[Direction.values().length];
	private int[] nextSendMin = new int[Direction.values().length];

	public PipeFluidInsertion(Item item) {
		super(item);
	}

	@Override
	public void enabledUpdateEntity() {
		super.enabledUpdateEntity();
		List<Integer> tempJamList = new ArrayList<>();
		if (!localJamList.isEmpty()) {
			List<Pair<Integer, Integer>> toRemove = new ArrayList<>();
			for (Pair<Integer, Integer> part : localJamList) {
				part.setValue2(part.getValue2() - 1);
				if (part.getValue2() <= 0) {
					toRemove.add(part);
				} else {
					tempJamList.add(part.getValue1());
				}
			}
			if (!toRemove.isEmpty()) {
				localJamList.removeAll(toRemove);
			}
		}
		PipeFluidTransportLogistics transport = (PipeFluidTransportLogistics) this.transport;
		for (Direction dir : Direction.values()) {
			FluidStack stack = transport.sideTanks[dir.ordinal()].getFluid();
			if (stack == null) {
				continue;
			}
			stack = stack.copy();

			if (nextSendMax[dir.ordinal()] > 0 && stack.amount < transport.sideTanks[dir.ordinal()].getCapacity()) {
				nextSendMax[dir.ordinal()]--;
				continue;
			}
			if (nextSendMin[dir.ordinal()] > 0) {
				nextSendMin[dir.ordinal()]--;
				continue;
			}

			Pair<Integer, FluidSinkReply> result = SimpleServiceLocator.logisticsFluidManager.getBestReply(FluidIdentifierStack.getFromStack(stack), getRouter(), tempJamList);
			if (result == null || result.getValue2().sinkAmount <= 0) {
				nextSendMax[dir.ordinal()] = 100;
				nextSendMin[dir.ordinal()] = 10;
				continue;
			}

			if (!useEnergy((int) (0.01 * result.getValue2().getSinkAmountInt()))) {
				nextSendMax[dir.ordinal()] = 100;
				nextSendMin[dir.ordinal()] = 10;
				continue;
			}

			FluidStack toSend = transport.sideTanks[dir.ordinal()].drain(result.getValue2().getSinkAmountInt(), true);
			ItemIdentifierStack liquidContainer = SimpleServiceLocator.logisticsFluidManager.getFluidContainer(FluidIdentifierStack.getFromStack(toSend));
			IRoutedItem routed = SimpleServiceLocator.routedItemHelper.createNewTravelItem(liquidContainer);
			routed.setDestination(result.getValue1());
			routed.setTransportMode(TransportMode.Passive);
			this.queueRoutedItem(routed, dir);
			nextSendMax[dir.ordinal()] = 100;
			nextSendMin[dir.ordinal()] = 5;
		}
	}

	@Override
	public void writeToNBT(@Nonnull CompoundNBT tag) {
		super.writeToNBT(tag);
		tag.putIntArray("nextSendMax", nextSendMax);
		tag.putIntArray("nextSendMin", nextSendMin);
	}

	@Override
	public void readFromNBT(@Nonnull CompoundNBT tag) {
		super.readFromNBT(tag);
		nextSendMax = tag.getIntArray("nextSendMax");
		if (nextSendMax.length < 6) {
			nextSendMax = new int[6];
		}
		nextSendMin = tag.getIntArray("nextSendMin");
		if (nextSendMin.length < 6) {
			nextSendMin = new int[6];
		}
	}

	@Override
	public TextureType getCenterTexture() {
		return Textures.LOGISTICSPIPE_LIQUID_INSERTION;
	}

	@Override
	public boolean canInsertToTanks() {
		return false;
	}

	@Override
	public boolean canInsertFromSideToTanks() {
		return false;
	}

	@Override
	public boolean canReceiveFluid() {
		return true;
	}
}
