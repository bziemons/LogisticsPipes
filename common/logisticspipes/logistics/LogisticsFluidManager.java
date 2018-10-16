package logisticspipes.logistics;

import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import net.minecraftforge.fluids.FluidStack;

import logisticspipes.LPItems;
import logisticspipes.interfaces.routing.IFluidSink;
import logisticspipes.items.LogisticsFluidContainer;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.routing.ExitRoute;
import logisticspipes.routing.IRouter;
import logisticspipes.routing.PipeRoutingConnectionType;
import logisticspipes.utils.FluidIdentifierStack;
import logisticspipes.utils.item.ItemIdentifierStack;
import logisticspipes.utils.tuples.Pair;

public class LogisticsFluidManager implements ILogisticsFluidManager {

	@Override
	public Pair<Integer, Integer> getBestReply(FluidIdentifierStack stack, IRouter sourceRouter, List<Integer> jamList) {
		for (ExitRoute candidateRouter : sourceRouter.getIRoutersByCost()) {
			if (!candidateRouter.containsFlag(PipeRoutingConnectionType.canRouteTo)) {
				continue;
			}
			if (candidateRouter.destination.getSimpleID() == sourceRouter.getSimpleID()) {
				continue;
			}
			if (jamList.contains(candidateRouter.destination.getSimpleID())) {
				continue;
			}

			if (candidateRouter.destination.getPipe() == null || !candidateRouter.destination.getPipe().isEnabled()) {
				continue;
			}
			CoreRoutedPipe pipe = candidateRouter.destination.getPipe();

			if (!(pipe instanceof IFluidSink)) {
				continue;
			}

			int amount = ((IFluidSink) pipe).sinkAmount(stack);
			if (amount > 0) {
				Pair<Integer, Integer> result = new Pair<>(candidateRouter.destination.getSimpleID(), amount);
				return result;
			}
		}
		Pair<Integer, Integer> result = new Pair<>(0, 0);
		return result;
	}

	@Override
	public ItemIdentifierStack getFluidContainer(FluidIdentifierStack stack) {
		ItemStack item = new ItemStack(LPItems.fluidContainer, 1);
		NBTTagCompound nbt = new NBTTagCompound();
		stack.makeFluidStack().writeToNBT(nbt);
		item.setTagCompound(nbt);
		return ItemIdentifierStack.getFromStack(item);
	}

	@Override
	public FluidIdentifierStack getFluidFromContainer(ItemIdentifierStack stack) {
		ItemStack itemStack = stack.makeNormalStack();
		if (itemStack.getItem() instanceof LogisticsFluidContainer && stack.getItem().tag != null) {
			return FluidIdentifierStack.getFromStack(FluidStack.loadFluidStackFromNBT(stack.getItem().tag));
		}
		return null;
	}
}
