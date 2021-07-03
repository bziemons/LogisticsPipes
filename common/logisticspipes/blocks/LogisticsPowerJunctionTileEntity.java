package logisticspipes.blocks;

import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.crash.CrashReportCategory;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.Direction;

import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;

import logisticspipes.LogisticsPipes;
import logisticspipes.api.ILogisticsPowerProvider;
import logisticspipes.config.Configs;
import logisticspipes.gui.hud.HUDPowerLevel;
import logisticspipes.interfaces.IBlockWatchingHandler;
import logisticspipes.interfaces.IGuiOpenControler;
import logisticspipes.interfaces.IGuiTileEntity;
import logisticspipes.interfaces.IHeadUpDisplayBlockRendererProvider;
import logisticspipes.interfaces.IHeadUpDisplayRenderer;
import logisticspipes.interfaces.IPowerLevelDisplay;
import logisticspipes.network.NewGuiHandler;
import logisticspipes.network.PacketHandler;
import logisticspipes.network.abstractguis.CoordinatesGuiProvider;
import logisticspipes.network.guis.block.PowerJunctionGui;
import logisticspipes.network.packets.block.PowerJunctionLevel;
import logisticspipes.network.packets.hud.HUDStartBlockWatchingPacket;
import logisticspipes.network.packets.hud.HUDStopBlockWatchingPacket;
import logisticspipes.proxy.MainProxy;
import logisticspipes.proxy.SimpleServiceLocator;
import logisticspipes.proxy.computers.interfaces.CCCommand;
import logisticspipes.proxy.computers.interfaces.CCType;
import logisticspipes.renderer.LogisticsHUDRenderer;
import logisticspipes.utils.PlayerCollectionList;

public class LogisticsPowerJunctionTileEntity extends LogisticsSolidTileEntity implements IGuiTileEntity, ILogisticsPowerProvider, IPowerLevelDisplay, IGuiOpenControler, IHeadUpDisplayBlockRendererProvider, IBlockWatchingHandler {

	public Object OPENPERIPHERAL_IGNORE; //Tell OpenPeripheral to ignore this class

	// true if it needs more power, turns off at full, turns on at 50%.
	public boolean needMorePowerTriggerCheck = true;

	public final static int RFDivisor = 2;
	public final static int MAX_STORAGE = 2000000;

	private int internalStorage = 0;
	private int lastUpdateStorage = 0;

	//small buffer to hold a fractional LP worth of RF
	private int internalRFbuffer = 0;

	private boolean addedToEnergyNet = false;

	private boolean init = false;
	private PlayerCollectionList guiListener = new PlayerCollectionList();
	private PlayerCollectionList watcherList = new PlayerCollectionList();
	private IHeadUpDisplayRenderer HUD;

	private final LazyOptional<IEnergyStorage> handler = LazyOptional.of(() -> new IEnergyStorage() {

		@Override
		public int receiveEnergy(int maxReceive, boolean simulate) {
			if (freeSpace() < 1) {
				return 0;
			}
			int RFspace = freeSpace() * LogisticsPowerJunctionTileEntity.RFDivisor - internalRFbuffer;
			int RFtotake = Math.min(maxReceive, RFspace);
			if (!simulate) {
				addEnergy(RFtotake / LogisticsPowerJunctionTileEntity.RFDivisor);
				internalRFbuffer += RFtotake % LogisticsPowerJunctionTileEntity.RFDivisor;
				if (internalRFbuffer >= LogisticsPowerJunctionTileEntity.RFDivisor) {
					addEnergy(1);
					internalRFbuffer -= LogisticsPowerJunctionTileEntity.RFDivisor;
				}
			}
			return RFtotake;
		}

		@Override
		public int extractEnergy(int maxExtract, boolean simulate) {
			return 0;
		}

		@Override
		public int getEnergyStored() {
			return internalStorage * LogisticsPowerJunctionTileEntity.RFDivisor + internalRFbuffer;
		}

		@Override
		public int getMaxEnergyStored() {
			return LogisticsPowerJunctionTileEntity.MAX_STORAGE * LogisticsPowerJunctionTileEntity.RFDivisor;
		}

		@Override
		public boolean canExtract() {
			return false;
		}

		@Override
		public boolean canReceive() {
			return true;
		}
	});

	private Object mjReceiver;

	public LogisticsPowerJunctionTileEntity() {
		HUD = new HUDPowerLevel(this);
	}

	@Override
	public boolean useEnergy(int amount, List<Object> providersToIgnore) {
		if (providersToIgnore != null && providersToIgnore.contains(this)) {
			return false;
		}
		if (canUseEnergy(amount, null)) {
			this.markDirty();
			internalStorage -= (int) ((amount * Configs.POWER_USAGE_MULTIPLIER) + 0.5D);
			if (internalStorage < LogisticsPowerJunctionTileEntity.MAX_STORAGE / 2) {
				needMorePowerTriggerCheck = true;
			}
			return true;
		}
		return false;
	}

	@Override
	public boolean canUseEnergy(int amount, List<Object> providersToIgnore) {
		if (providersToIgnore != null && providersToIgnore.contains(this)) {
			return false;
		}
		return internalStorage >= (int) ((amount * Configs.POWER_USAGE_MULTIPLIER) + 0.5D);
	}

	@Override
	public boolean useEnergy(int amount) {
		return useEnergy(amount, null);
	}

	public int freeSpace() {
		return LogisticsPowerJunctionTileEntity.MAX_STORAGE - internalStorage;
	}

	public void updateClients() {
		MainProxy.sendToPlayerList(PacketHandler.getPacket(PowerJunctionLevel.class).setInteger(internalStorage).setBlockPos(pos), guiListener);
		MainProxy.sendToPlayerList(PacketHandler.getPacket(PowerJunctionLevel.class).setInteger(internalStorage).setBlockPos(pos), watcherList);
		lastUpdateStorage = internalStorage;
	}

	@Override
	public boolean canUseEnergy(int amount) {
		return canUseEnergy(amount, null);
	}

	public void addEnergy(float amount) {
		if (MainProxy.isClient(getWorld())) {
			return;
		}
		internalStorage += amount;
		if (internalStorage > LogisticsPowerJunctionTileEntity.MAX_STORAGE) {
			internalStorage = LogisticsPowerJunctionTileEntity.MAX_STORAGE;
		}
		if (internalStorage == LogisticsPowerJunctionTileEntity.MAX_STORAGE) {
			needMorePowerTriggerCheck = false;
		}
		this.markDirty();
	}

	@Override
	public void readFromNBT(CompoundNBT tag) {
		super.readFromNBT(tag);
		internalStorage = tag.getInt("powerLevel");
		if (tag.contains("needMorePowerTriggerCheck")) {
			needMorePowerTriggerCheck = tag.getBoolean("needMorePowerTriggerCheck");
		}
	}

	@Nonnull
	@Override
	public CompoundNBT writeToNBT(CompoundNBT tag) {
		tag = super.writeToNBT(tag);
		tag.putInt("powerLevel", internalStorage);
		tag.putBoolean("needMorePowerTriggerCheck", needMorePowerTriggerCheck);
		return tag;
	}

	@Override
	public void update() {
		super.update();
		if (MainProxy.isServer(getWorld())) {
			if (internalStorage != lastUpdateStorage) {
				updateClients();
			}
		}
		if (!init) {
			if (MainProxy.isClient(getWorld())) {
				LogisticsHUDRenderer.instance().add(this);
			}
			if (!addedToEnergyNet) {
				SimpleServiceLocator.IC2Proxy.registerToEneryNet(this);
				addedToEnergyNet = true;
			}
			init = true;
		}
	}

	@Override
	public void invalidate() {
		super.invalidate();
		if (MainProxy.isClient(getWorld())) {
			LogisticsHUDRenderer.instance().remove(this);
		}
		if (addedToEnergyNet) {
			SimpleServiceLocator.IC2Proxy.unregisterToEneryNet(this);
			addedToEnergyNet = false;
		}
	}

	@Override
	public void validate() {
		super.validate();
		if (MainProxy.isClient(getWorld())) {
			init = false;
		}
		if (!addedToEnergyNet) {
			init = false;
		}
	}

	@Override
	public void onChunkUnloaded() {
		super.onChunkUnloaded();
		if (MainProxy.isClient(getWorld())) {
			LogisticsHUDRenderer.instance().remove(this);
		}
		if (addedToEnergyNet) {
			SimpleServiceLocator.IC2Proxy.unregisterToEneryNet(this);
			addedToEnergyNet = false;
		}
	}

	@Override
	public int getPowerLevel() {
		return internalStorage;
	}

	@Override
	public int getDisplayPowerLevel() {
		return getPowerLevel();
	}

	@Override
	public String getBrand() {
		return "LP";
	}

	@Override
	public int getMaxStorage() {
		return LogisticsPowerJunctionTileEntity.MAX_STORAGE;
	}

	@Override
	public int getChargeState() {
		return internalStorage * 100 / LogisticsPowerJunctionTileEntity.MAX_STORAGE;
	}

	@Override
	public void guiOpenedByPlayer(PlayerEntity player) {
		guiListener.add(player);
		updateClients();
	}

	@Override
	public void guiClosedByPlayer(PlayerEntity player) {
		guiListener.remove(player);
	}

	public void handlePowerPacket(int integer) {
		if (MainProxy.isClient(getWorld())) {
			internalStorage = integer;
		}
	}

	@Override
	public IHeadUpDisplayRenderer getRenderer() {
		return HUD;
	}

	@Override
	public int getX() {
		return pos.getX();
	}

	@Override
	public int getY() {
		return pos.getY();
	}

	@Override
	public int getZ() {
		return pos.getZ();
	}

	@Override
	public void startWatching() {
		MainProxy.sendPacketToServer(PacketHandler.getPacket(HUDStartBlockWatchingPacket.class).setPosX(getX()).setPosY(getY()).setPosZ(getZ()));
	}

	@Override
	public void stopWatching() {
		MainProxy.sendPacketToServer(PacketHandler.getPacket(HUDStopBlockWatchingPacket.class).setPosX(getX()).setPosY(getY()).setPosZ(getZ()));
	}

	@Override
	public void playerStartWatching(PlayerEntity player) {
		watcherList.add(player);
		updateClients();
	}

	@Override
	public void playerStopWatching(PlayerEntity player) {
		watcherList.remove(player);
	}

	@Override
	public boolean isHUDExistent() {
		return getWorld().getTileEntity(pos) == this;
	}

	@Override
	public void addInfoToCrashReport(CrashReportCategory par1CrashReportCategory) {
		super.addInfoToCrashReport(par1CrashReportCategory);
		par1CrashReportCategory.func_71507_a("LP-Version", LogisticsPipes.getVersionString());
	}

	@Override
	public boolean isHUDInvalid() {
		return isRemoved();
	}

	@Nonnull
	@Override
	public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
		final LazyOptional<T> opt = CapabilityEnergy.ENERGY.orEmpty(cap, handler);
		if (opt.isPresent()) return opt;
		return super.getCapability(cap, side);
	}

	@Override
	public CoordinatesGuiProvider getGuiProvider() {
		return NewGuiHandler.getGui(PowerJunctionGui.class);
	}
}
