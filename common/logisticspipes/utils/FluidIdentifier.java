package logisticspipes.utils;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.IntStream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.fluid.Fluid;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.Registry;

import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.IFluidTank;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;

import lombok.AllArgsConstructor;
import org.jetbrains.annotations.Contract;

import logisticspipes.asm.addinfo.IAddInfo;
import logisticspipes.asm.addinfo.IAddInfoProvider;
import logisticspipes.proxy.SimpleServiceLocator;
import logisticspipes.utils.item.ItemIdentifier;
import logisticspipes.utils.item.ItemIdentifierStack;

public class FluidIdentifier implements Comparable<FluidIdentifier> {

	private final static ReadWriteLock dblock = new ReentrantReadWriteLock();
	private final static Lock rlock = FluidIdentifier.dblock.readLock();
	private final static Lock wlock = FluidIdentifier.dblock.writeLock();

	//map uniqueID -> FluidIdentifier
	private final static HashMap<Integer, FluidIdentifier> _fluidIdentifierIdCache = new HashMap<>(256, 0.5f);

	//for fluids with tags, map fluidID -> map tag -> FluidIdentifier
	private final static Map<ResourceLocation, HashMap<FinalCompoundNBT, FluidIdentifier>> _fluidIdentifierTagCache = new HashMap<>(
			256);

	//for fluids without tags, map fluidID -> FluidIdentifier
	private final static Map<ResourceLocation, FluidIdentifier> _fluidIdentifierCache = new HashMap<>(256);
	private static boolean init = false;
	public final ResourceLocation fluidResource;
	public final FinalCompoundNBT tag;
	public final int uniqueID;

	private FluidIdentifier(@Nonnull ResourceLocation fluidResource, @Nullable FinalCompoundNBT tag, int uniqueID) {
		this.fluidResource = Objects.requireNonNull(fluidResource, "fluid resource may not be null");
		this.tag = tag;
		this.uniqueID = uniqueID;
	}

	public static FluidIdentifier get(@Nonnull Fluid fluid, @Nullable CompoundNBT tag, @Nullable FluidIdentifier proposal) {
		final ResourceLocation fluidResource = fluid.getRegistryName();
		Objects.requireNonNull(fluidResource, "Fluid '" + fluid + "' not in registry");
		if (tag == null) {
			if (proposal != null) {
				if (proposal.fluidResource.equals(fluidResource) && proposal.tag == null) {
					return proposal;
				}
			}
			proposal = null;
			IAddInfoProvider prov = null;
			if (fluid instanceof IAddInfoProvider) {
				prov = (IAddInfoProvider) fluid;
				FluidAddInfo info = prov.getLogisticsPipesAddInfo(FluidAddInfo.class);
				if (info != null) {
					proposal = info.fluid;
				}
			}
			FluidIdentifier ident = getFluidIdentifierWithoutTag(fluid, fluidResource, proposal);
			if (proposal != ident && prov != null) {
				prov.setLogisticsPipesAddInfo(new FluidAddInfo(ident));
			}
			return ident;
		} else {
			FluidIdentifier.rlock.lock();
			{
				HashMap<FinalCompoundNBT, FluidIdentifier> fluidNBTList = FluidIdentifier._fluidIdentifierTagCache
						.get(fluidResource);
				if (fluidNBTList != null) {
					FinalCompoundNBT tagwithfixedname = new FinalCompoundNBT(tag);
					FluidIdentifier unknownFluid = fluidNBTList.get(tagwithfixedname);
					if (unknownFluid != null) {
						FluidIdentifier.rlock.unlock();
						return unknownFluid;
					}
				}
			}
			FluidIdentifier.rlock.unlock();
			FluidIdentifier.wlock.lock();
			{
				HashMap<FinalCompoundNBT, FluidIdentifier> fluidNBTList =
						FluidIdentifier._fluidIdentifierTagCache.get(fluidResource);
				if (fluidNBTList != null) {
					FinalCompoundNBT tagwithfixedname = new FinalCompoundNBT(tag);
					FluidIdentifier unknownFluid = fluidNBTList.get(tagwithfixedname);
					if (unknownFluid != null) {
						FluidIdentifier.wlock.unlock();
						return unknownFluid;
					}
				}
			}
			HashMap<FinalCompoundNBT, FluidIdentifier> fluidNBTList = FluidIdentifier._fluidIdentifierTagCache
					.computeIfAbsent(fluidResource, k -> new HashMap<>(16, 0.5f));
			FinalCompoundNBT finaltag = new FinalCompoundNBT(tag);
			int id = FluidIdentifier.getUnusedId();
			FluidIdentifier unknownFluid = new FluidIdentifier(fluidResource, finaltag, id);
			fluidNBTList.put(finaltag, unknownFluid);
			FluidIdentifier._fluidIdentifierIdCache.put(id, unknownFluid);
			FluidIdentifier.wlock.unlock();
			return (unknownFluid);
		}
	}

	private static FluidIdentifier getFluidIdentifierWithoutTag(@Nonnull Fluid fluid,
			@Nonnull ResourceLocation fluidResource,
			@Nullable FluidIdentifier proposal) {
		if (proposal != null) {
			if (proposal.fluidResource.equals(fluidResource) && proposal.tag == null) {
				return proposal;
			}
		}
		FluidIdentifier.rlock.lock();
		{
			FluidIdentifier unknownFluid = FluidIdentifier._fluidIdentifierCache.get(fluidResource);
			if (unknownFluid != null) {
				FluidIdentifier.rlock.unlock();
				return unknownFluid;
			}
		}
		FluidIdentifier.rlock.unlock();
		FluidIdentifier.wlock.lock();
		{
			FluidIdentifier unknownFluid = FluidIdentifier._fluidIdentifierCache.get(fluidResource);
			if (unknownFluid != null) {
				FluidIdentifier.wlock.unlock();
				return unknownFluid;
			}
		}
		int id = FluidIdentifier.getUnusedId();
		FluidIdentifier unknownFluid = new FluidIdentifier(fluidResource, null, id);
		FluidIdentifier._fluidIdentifierCache.put(fluidResource, unknownFluid);
		FluidIdentifier._fluidIdentifierIdCache.put(id, unknownFluid);
		FluidIdentifier.wlock.unlock();
		return (unknownFluid);
	}

	@Contract("null -> null")
	@Nullable
	public static FluidIdentifier get(@Nullable FluidStack stack) {
		if (stack == null) {
			return null;
		}
		FluidIdentifier proposal = null;
		IAddInfoProvider prov = null;
		if (stack instanceof IAddInfoProvider) {
			prov = (IAddInfoProvider) stack;
			FluidStackAddInfo info = prov.getLogisticsPipesAddInfo(FluidStackAddInfo.class);
			if (info != null) {
				proposal = info.fluid;
			}
		}
		FluidIdentifier ident = FluidIdentifier.get(stack.getFluid(), stack.getTag(), proposal);
		if (proposal != ident && !stack.hasTag() && prov != null) {
			prov.setLogisticsPipesAddInfo(new FluidStackAddInfo(ident));
		}
		return ident;
	}

	@Nullable
	public static FluidIdentifier get(ItemIdentifier stack) {
		return FluidIdentifier.get(stack.makeStack(1));
	}

	@Nullable
	public static FluidIdentifier get(@Nonnull ItemStack stack) {
		return FluidIdentifier.get(ItemIdentifierStack.getFromStack(stack));
	}

	@Nullable
	public static FluidIdentifier get(ItemIdentifierStack stack) {
		FluidStack f = null;
		FluidIdentifierStack fstack = SimpleServiceLocator.logisticsFluidManager.getFluidFromContainer(stack);
		if (fstack != null) {
			f = fstack.makeFluidStack();
		} else {
			ItemStack itemStack = stack.unsafeMakeNormalStack();

			f = itemStack.getCapability(CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY, null)
					.map(fluidHandler ->
							IntStream.range(0, fluidHandler.getTanks())
									.mapToObj(fluidHandler::getFluidInTank)
									.filter(fluidStack -> !fluidStack.isEmpty())
									.findFirst()
					)
					.orElse(Optional.empty())
					.orElse(null);
		}
		if (f == null) {
			f = FluidUtil.getFluidContained(stack.unsafeMakeNormalStack()).orElse(null);
		}

		if (f == null) {
			return null;
		} else {
			return FluidIdentifier.get(f);
		}
	}

	private static FluidIdentifier get(Fluid fluid) {
		return FluidIdentifier.get(fluid, null, null);
	}

	private static int getUnusedId() {
		int id = new Random().nextInt();
		while (FluidIdentifier.isIdUsed(id)) {
			id = new Random().nextInt();
		}
		return id;
	}

	private static boolean isIdUsed(int id) {
		return FluidIdentifier._fluidIdentifierIdCache.containsKey(id);
	}

	public static void initFromForge(boolean flag) {
		if (FluidIdentifier.init) {
			return;
		}
		Registry.FLUID.stream().forEach(FluidIdentifier::get);
		if (flag) {
			FluidIdentifier.init = true;
		}
	}

	@Nullable
	public static FluidIdentifier first() {
		FluidIdentifier.rlock.lock();
		for (FluidIdentifier i : FluidIdentifier._fluidIdentifierCache.values()) {
			if (i != null) {
				FluidIdentifier.rlock.unlock();
				return i;
			}
		}
		FluidIdentifier.rlock.unlock();
		return null;
	}

	public static FluidIdentifier last() {
		FluidIdentifier.rlock.lock();
		FluidIdentifier last = null;
		for (FluidIdentifier i : FluidIdentifier._fluidIdentifierCache.values()) {
			if (i != null) {
				last = i;
			}
		}
		FluidIdentifier.rlock.unlock();
		return last;
	}

	public static Collection<FluidIdentifier> all() {
		FluidIdentifier.rlock.lock();
		Collection<FluidIdentifier> list = Collections
				.unmodifiableCollection(FluidIdentifier._fluidIdentifierCache.values());
		FluidIdentifier.rlock.unlock();
		return list;
	}

	@Override
	public int compareTo(FluidIdentifier o) {
		int c = fluidResource.compareTo(o.fluidResource);
		if (c != 0) {
			return c;
		}
		c = uniqueID - o.uniqueID;
		return c;
	}

	@Nonnull
	public FluidStack makeFluidStack(int amount) {
		//FluidStack constructor does the tag.copy(), so this is safe
		return new FluidStack(getFluidResource(), amount, tag);
	}

	public FluidIdentifierStack makeFluidIdentifierStack(int amount) {
		//FluidStack constructor does the tag.copy(), so this is safe
		return new FluidIdentifierStack(this, amount);
	}

	public Fluid getFluidResource() {
		return Registry.FLUID.getOrDefault(fluidResource);
	}

	public int getFreeSpaceInsideTank(IFluidTank tank) {
		FluidStack liquid = tank.getFluid();
		if (liquid.isEmpty() || liquid.getFluid() == null) {
			return tank.getCapacity();
		}
		if (equals(FluidIdentifier.get(liquid))) {
			return tank.getCapacity() - liquid.getAmount();
		}
		return 0;
	}

	@Override
	public String toString() {
		String t = tag != null ? tag.toString() : "null";
		return fluidResource + ":" + t;
	}

	@Nullable
	public FluidIdentifier next() {
		FluidIdentifier.rlock.lock();
		boolean takeNext = false;
		for (FluidIdentifier i : FluidIdentifier._fluidIdentifierCache.values()) {
			if (takeNext && i != null) {
				FluidIdentifier.rlock.unlock();
				return i;
			}
			if (equals(i)) {
				takeNext = true;
			}
		}
		FluidIdentifier.rlock.unlock();
		return null;
	}

	@Nullable
	public FluidIdentifier prev() {
		FluidIdentifier.rlock.lock();
		FluidIdentifier last = null;
		for (FluidIdentifier i : FluidIdentifier._fluidIdentifierCache.values()) {
			if (equals(i)) {
				FluidIdentifier.rlock.unlock();
				return last;
			}
			if (i != null) {
				last = i;
			}
		}
		FluidIdentifier.rlock.unlock();
		return last;
	}

	public ItemIdentifier getItemIdentifier() {
		return SimpleServiceLocator.logisticsFluidManager.getFluidContainer(this.makeFluidIdentifierStack(1)).getItem();
	}

	@AllArgsConstructor
	private static class FluidStackAddInfo implements IAddInfo {

		private final FluidIdentifier fluid;
	}

	@AllArgsConstructor
	private static class FluidAddInfo implements IAddInfo {

		private final FluidIdentifier fluid;
	}

}
