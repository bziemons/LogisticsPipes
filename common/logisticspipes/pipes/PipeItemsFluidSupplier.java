package logisticspipes.pipes;

import java.util.HashMap;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;

import logisticspipes.LogisticsPipes;
import logisticspipes.interfaces.ITankUtil;
import logisticspipes.modules.abstractmodules.LogisticsModule;
import logisticspipes.network.GuiIDs;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.proxy.MainProxy;
import logisticspipes.proxy.SimpleServiceLocator;
import logisticspipes.textures.Textures;
import logisticspipes.textures.Textures.TextureType;
import logisticspipes.transport.LPTravelingItem.LPTravelingItemServer;
import logisticspipes.transport.PipeTransportLogistics;
import logisticspipes.utils.CacheHolder.CacheTypes;
import logisticspipes.utils.FluidIdentifierStack;
import logisticspipes.utils.item.ItemIdentifier;
import logisticspipes.utils.item.ItemIdentifierInventory;

public class PipeItemsFluidSupplier extends CoreRoutedPipe {

	public PipeItemsFluidSupplier(Item item) {
		super(new PipeTransportLogistics(true) {

			@Override
			public boolean canPipeConnect(TileEntity tile, EnumFacing dir) {
				if (super.canPipeConnect(tile, dir)) {
					return true;
				}
				if (SimpleServiceLocator.pipeInformationManager.isItemPipe(tile)) {
					return false;
				}
				//TODO: FIXME
				/*
				if (tile instanceof IFluidHandler) {
					IFluidHandler liq = (IFluidHandler) tile;
					if (liq.getTankInfo(dir.getOpposite()) != null && liq.getTankInfo(dir.getOpposite()).length > 0) {
						return true;
					}
				}
				*/
				return false;
			}
		}, item);

		throttleTime = 100;
	}

	@Override
	public TextureType getCenterTexture() {
		return Textures.LOGISTICSPIPE_LIQUIDSUPPLIER_TEXTURE;
	}

	@Override
	public LogisticsModule getLogisticsModule() {
		return null;
	}

	@Override
	public ItemSendMode getItemSendMode() {
		return ItemSendMode.Fast;
	}

	public void endReached(LPTravelingItemServer data, TileEntity tile) {
		getCacheHolder().trigger(CacheTypes.Inventory);
		transport.markChunkModified(tile);
		notifyOfItemArival(data.getInfo());
		EnumFacing orientation = data.output.getOpposite();
		if (getOriginalUpgradeManager().hasSneakyUpgrade()) {
			orientation = getOriginalUpgradeManager().getSneakyOrientation();
		}
		ITankUtil util = SimpleServiceLocator.tankUtilFactory.getTankUtilForTE(tile, orientation);
		if (util == null) {
			return;
		}
		if (SimpleServiceLocator.pipeInformationManager.isItemPipe(tile)) {
			return;
		}
		if (data.getItemIdentifierStack() == null) {
			return;
		}
		FluidIdentifierStack liquidId = FluidIdentifierStack.getFromStack(data.getItemIdentifierStack());
		if (liquidId == null) {
			return;
		}
		while (data.getItemIdentifierStack().getStackSize() > 0 && util.fill(liquidId, false) == liquidId.getAmount() && this.useEnergy(5)) {
			util.fill(liquidId, true);
			data.getItemIdentifierStack().lowerStackSize(1);
			Item item = data.getItemIdentifierStack().getItem().item;
			if (item.hasContainerItem(data.getItemIdentifierStack().makeNormalStack())) {
				Item containerItem = item.getContainerItem();
				transport.sendItem(new ItemStack(containerItem, 1));
			}
		}
	}

	@Override
	public boolean hasGenericInterests() {
		return true;
	}

	// from PipeItemsFluidSupplier
	private ItemIdentifierInventory dummyInventory = new ItemIdentifierInventory(9, "Fluids to keep stocked", 127);

	private final HashMap<ItemIdentifier, Integer> _requestedItems = new HashMap<>();

	private boolean _requestPartials = false;

	@Override
	public void throttledUpdateEntity() {
		if (!isEnabled()) {
			return;
		}

		if (MainProxy.isClient(getWorld())) {
			return;
		}
		super.throttledUpdateEntity();

		// TODO PROVIDE REFACTOR: request needed fluids
	}

	@Override
	public void readFromNBT(NBTTagCompound nbttagcompound) {
		super.readFromNBT(nbttagcompound);
		dummyInventory.readFromNBT(nbttagcompound, "");
		_requestPartials = nbttagcompound.getBoolean("requestpartials");
	}

	@Override
	public void writeToNBT(NBTTagCompound nbttagcompound) {
		super.writeToNBT(nbttagcompound);
		dummyInventory.writeToNBT(nbttagcompound, "");
		nbttagcompound.setBoolean("requestpartials", _requestPartials);
	}

	public boolean isRequestingPartials() {
		return _requestPartials;
	}

	public void setRequestingPartials(boolean value) {
		_requestPartials = value;
	}

	@Override
	public void onWrenchClicked(EntityPlayer entityplayer) {
		entityplayer.openGui(LogisticsPipes.instance, GuiIDs.GUI_FluidSupplier_ID, getWorld(), getX(), getY(), getZ());
	}

	/*** GUI ***/
	public ItemIdentifierInventory getDummyInventory() {
		return dummyInventory;
	}
}
