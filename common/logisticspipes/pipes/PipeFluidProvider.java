package logisticspipes.pipes;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;

import net.minecraft.item.Item;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;

import net.minecraftforge.fluids.FluidStack;

import logisticspipes.pipes.basic.fluid.FluidRoutedPipe;
import logisticspipes.proxy.SimpleServiceLocator;
import logisticspipes.textures.Textures;
import logisticspipes.textures.Textures.TextureType;
import logisticspipes.utils.FluidIdentifier;
import logisticspipes.utils.FluidIdentifierStack;
import logisticspipes.utils.item.ItemIdentifierStack;
import logisticspipes.utils.tuples.Triplet;

public class PipeFluidProvider extends FluidRoutedPipe implements IProvideFluids {

	public PipeFluidProvider(Item item) {
		super(item);
	}

	@Override
	public boolean disconnectPipe(TileEntity tile, EnumFacing dir) {
		return SimpleServiceLocator.pipeInformationManager.isFluidPipe(tile);
	}

	@Override
	public void enabledUpdateEntity() {
		super.enabledUpdateEntity();
		if (!getFluidOrderManager().hasOrders(ResourceType.PROVIDER) || !isNthTick(6)) {
			return;
		}

		LogisticsFluidOrder order = getFluidOrderManager().peekAtTopRequest(ResourceType.PROVIDER);
		AtomicInteger amountToSend = new AtomicInteger();
		AtomicInteger attemptedAmount = new AtomicInteger();
		amountToSend.set(Math.min(order.getAmount(), 5000));
		attemptedAmount.set(Math.min(order.getAmount(), 5000));
		for (Triplet<ITankUtil, TileEntity, EnumFacing> pair : getAdjacentTanksAdvanced(false)) {
			if (amountToSend.get() <= 0) {
				break;
			}
			ITankUtil util = pair.getValue1();
			boolean fallback = true;
			if (util instanceof ISpecialTankUtil) {
				fallback = false;
				ISpecialTankAccessHandler handler = ((ISpecialTankUtil) util).getSpecialHandler();
				TileEntity tile = ((ISpecialTankUtil) util).getTileEntity();
				FluidStack drained = handler.drainFrom(tile, order.getFluid(), amountToSend.get(), false);
				if (drained != null && drained.amount > 0 && order.getFluid().equals(FluidIdentifier.get(drained))) {
					drained = handler.drainFrom(tile, order.getFluid(), amountToSend.get(), true);
					int amount = drained.amount;
					amountToSend.addAndGet(-amount);
					ItemIdentifierStack stack = SimpleServiceLocator.logisticsFluidManager.getFluidContainer(FluidIdentifierStack.getFromStack(drained));
					IRoutedItem item = SimpleServiceLocator.routedItemHelper.createNewTravelItem(stack);
					item.setDestination(order.getRouter().getSimpleID());
					item.setTransportMode(TransportMode.Active);
					this.queueRoutedItem(item, pair.getValue3());
					getFluidOrderManager().sendSuccessfull(amount, false, item);
					if (amountToSend.get() <= 0) {
						break;
					}
				}
			}
			if (fallback) {
				if (util.containsTanks()) {
					util.forEachFluid(fluidStack -> {
						if (amountToSend.get() <= 0) {
							return;
						}
						if (fluidStack.getFluid() != null) {
							if (order.getFluid().equals(fluidStack.getFluid())) {
								int amount = Math.min(fluidStack.getAmount(), amountToSend.get());
								FluidIdentifierStack drained = util.drain(amount, false);
								if (drained != null && drained.getAmount() > 0 && order.getFluid().equals(drained.getFluid())) {
									drained = util.drain(amount, true);
									while (drained.getAmount() < amountToSend.get()) {
										FluidIdentifierStack addition = util.drain(amountToSend.get() - drained.getAmount(), false);
										if (addition != null && addition.getAmount() > 0 && order.getFluid().equals(addition.getFluid())) {
											addition = util.drain(amountToSend.get() - drained.getAmount(), true);
											drained.raiseAmount(addition.getAmount());
										} else {
											break;
										}
									}
									amount = drained.getAmount();
									amountToSend.addAndGet(-amount);
									ItemIdentifierStack stack = SimpleServiceLocator.logisticsFluidManager.getFluidContainer(drained);
									IRoutedItem item = SimpleServiceLocator.routedItemHelper.createNewTravelItem(stack);
									item.setDestination(order.getRouter().getSimpleID());
									item.setTransportMode(TransportMode.Active);
									queueRoutedItem(item, pair.getValue3());
									getFluidOrderManager().sendSuccessfull(amount, false, item);
								}
							}
						}
					});
				}
			}
		}
		if (amountToSend.get() >= attemptedAmount.get()) {
			getFluidOrderManager().sendFailed();
		}
	}

	@Override
	public Map<FluidIdentifier, Integer> getAvailableFluids() {
		Map<FluidIdentifier, Integer> map = new HashMap<>();
		for (Triplet<ITankUtil, TileEntity, EnumFacing> pair : getAdjacentTanksAdvanced(false)) {
			ITankUtil util = pair.getValue1();
			boolean fallback = true;
			if (util instanceof ISpecialTankUtil) {
				fallback = false;
				ISpecialTankAccessHandler handler = ((ISpecialTankUtil) util).getSpecialHandler();
				TileEntity tile = ((ISpecialTankUtil) util).getTileEntity();
				Map<FluidIdentifier, Long> tmp = handler.getAvailableLiquid(tile);
				for (Entry<FluidIdentifier, Long> entry : tmp.entrySet()) {
					if (map.containsKey(entry.getKey())) {
						long addition = ((long) map.get(entry.getKey())) + entry.getValue();
						map.put(entry.getKey(), addition > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) addition);
					} else {
						map.put(entry.getKey(), entry.getValue() > Integer.MAX_VALUE ? Integer.MAX_VALUE : entry.getValue().intValue());
					}
				}
			}
			if (fallback) {
				if (util.containsTanks()) {
					util.forEachFluid(liquid -> {
						if (liquid.getFluid() != null) {
							FluidIdentifier ident = liquid.getFluid();
							if (util.canDrain(ident)) {
								if (util.drain(ident.makeFluidIdentifierStack(1), false) != null) {
									if (map.containsKey(ident)) {
										long addition = ((long) map.get(ident)) + liquid.getAmount();
										map.put(ident, addition > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) addition);
									} else {
										map.put(ident, liquid.getAmount());
									}
								}
							}
						}
					});
				}
			}
		}
		Map<FluidIdentifier, Integer> result = new HashMap<>();
		//Reduce what has been reserved, add.
		for (Entry<FluidIdentifier, Integer> fluid : map.entrySet()) {
			int remaining = fluid.getValue() - getFluidOrderManager().totalFluidsCountInOrders(fluid.getKey());
			if (remaining < 1) {
				continue;
			}
			result.put(fluid.getKey(), remaining);
		}
		return result;
	}

	@Override
	public TextureType getCenterTexture() {
		return Textures.LOGISTICSPIPE_LIQUID_PROVIDER;
	}

	@Override
	public boolean canInsertToTanks() {
		return true;
	}

	@Override
	public boolean canInsertFromSideToTanks() {
		return true;
	}

	@Override
	public boolean canReceiveFluid() {
		return false;
	}
}
