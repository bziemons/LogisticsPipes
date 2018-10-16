package logisticspipes.pipes;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.nbt.NBTTagCompound;

import logisticspipes.LogisticsPipes;
import logisticspipes.gui.hud.HUDSatellite;
import logisticspipes.interfaces.IChestContentReceiver;
import logisticspipes.interfaces.IHeadUpDisplayRenderer;
import logisticspipes.interfaces.IHeadUpDisplayRendererProvider;
import logisticspipes.interfaces.ITankUtil;
import logisticspipes.modules.ModuleSatellite;
import logisticspipes.modules.abstractmodules.LogisticsModule;
import logisticspipes.network.GuiIDs;
import logisticspipes.network.PacketHandler;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.network.packets.hud.ChestContent;
import logisticspipes.network.packets.hud.HUDStartWatchingPacket;
import logisticspipes.network.packets.hud.HUDStopWatchingPacket;
import logisticspipes.network.packets.satpipe.SatPipeNext;
import logisticspipes.network.packets.satpipe.SatPipePrev;
import logisticspipes.network.packets.satpipe.SatPipeSetID;
import logisticspipes.pipes.basic.fluid.FluidRoutedPipe;
import logisticspipes.proxy.MainProxy;
import logisticspipes.textures.Textures;
import logisticspipes.textures.Textures.TextureType;
import logisticspipes.utils.FluidIdentifier;
import logisticspipes.utils.PlayerCollectionList;
import logisticspipes.utils.item.ItemIdentifierStack;

public class PipeFluidSatellite extends FluidRoutedPipe implements IHeadUpDisplayRendererProvider, IChestContentReceiver {

	public final PlayerCollectionList localModeWatchers = new PlayerCollectionList();
	public final LinkedList<ItemIdentifierStack> itemList = new LinkedList<>();
	public final LinkedList<ItemIdentifierStack> oldList = new LinkedList<>();
	private final HUDSatellite HUD = new HUDSatellite(this);

	public PipeFluidSatellite(Item item) {
		super(item);
		throttleTime = 40;
	}

	@Override
	public boolean canInsertFromSideToTanks() {
		return true;
	}

	@Override
	public boolean canInsertToTanks() {
		return true;
	}

	@Override
	public TextureType getCenterTexture() {
		return Textures.LOGISTICSPIPE_LIQUID_SATELLITE;
	}

	@Override
	public LogisticsModule getLogisticsModule() {
		return new ModuleSatellite(this);
	}

	@Override
	public ItemSendMode getItemSendMode() {
		return ItemSendMode.Normal;
	}

	@Override
	public void enabledUpdateEntity() {
		super.enabledUpdateEntity();
		if (isNthTick(20) && localModeWatchers.size() > 0) {
			updateInv(false);
		}
	}

	private void addToList(ItemIdentifierStack stack) {
		for (ItemIdentifierStack ident : itemList) {
			if (ident.getItem().equals(stack.getItem())) {
				ident.setStackSize(ident.getStackSize() + stack.getStackSize());
				return;
			}
		}
		itemList.addLast(stack);
	}

	private void updateInv(boolean force) {
		itemList.clear();
		for (ITankUtil util : getAdjacentTanks(false)) {
			util.forEachFluid(liquid -> {
				if (liquid != null && liquid.getFluid() != null) {
					addToList(liquid.getFluid().getItemIdentifier().makeStack(liquid.getAmount()));
				}
			});
		}
		if (!itemList.equals(oldList) || force) {
			oldList.clear();
			oldList.addAll(itemList);
			MainProxy.sendToPlayerList(PacketHandler.getPacket(ChestContent.class).setIdentList(itemList).setPosX(getX()).setPosY(getY()).setPosZ(getZ()), localModeWatchers);
		}
	}

	@Override
	public void setReceivedChestContent(Collection<ItemIdentifierStack> list) {
		itemList.clear();
		itemList.addAll(list);
	}

	@Override
	public IHeadUpDisplayRenderer getRenderer() {
		return HUD;
	}

	@Override
	public void startWatching() {
		MainProxy.sendPacketToServer(PacketHandler.getPacket(HUDStartWatchingPacket.class).setInteger(1).setPosX(getX()).setPosY(getY()).setPosZ(getZ()));
	}

	@Override
	public void stopWatching() {
		MainProxy.sendPacketToServer(PacketHandler.getPacket(HUDStopWatchingPacket.class).setInteger(1).setPosX(getX()).setPosY(getY()).setPosZ(getZ()));
	}

	@Override
	public void playerStartWatching(EntityPlayer player, int mode) {
		if (mode == 1) {
			localModeWatchers.add(player);
			final ModernPacket packet = PacketHandler.getPacket(SatPipeSetID.class).setSatID((this).satelliteId).setPosX(getX()).setPosY(getY()).setPosZ(getZ());
			MainProxy.sendPacketToPlayer(packet, player);
			updateInv(true);
		} else {
			super.playerStartWatching(player, mode);
		}
	}

	@Override
	public void playerStopWatching(EntityPlayer player, int mode) {
		super.playerStopWatching(player, mode);
		localModeWatchers.remove(player);
	}

	// from baseLogicLiquidSatellite
	public static HashSet<PipeFluidSatellite> AllSatellites = new HashSet<>();

	// called only on server shutdown
	public static void cleanup() {
		PipeFluidSatellite.AllSatellites.clear();
	}

	protected final Map<FluidIdentifier, Integer> _lostItems = new HashMap<>();

	public int satelliteId;

	@Override
	public void readFromNBT(NBTTagCompound nbttagcompound) {
		super.readFromNBT(nbttagcompound);
		satelliteId = nbttagcompound.getInteger("satelliteid");
		ensureAllSatelliteStatus();
	}

	@Override
	public void writeToNBT(NBTTagCompound nbttagcompound) {
		nbttagcompound.setInteger("satelliteid", satelliteId);
		super.writeToNBT(nbttagcompound);
	}

	protected int findId(int increment) {
		if (MainProxy.isClient(getWorld())) {
			return satelliteId;
		}
		int potentialId = satelliteId;
		boolean conflict = true;
		while (conflict) {
			potentialId += increment;
			if (potentialId < 0) {
				return 0;
			}
			conflict = false;
			for (final PipeFluidSatellite sat : PipeFluidSatellite.AllSatellites) {
				if (sat.satelliteId == potentialId) {
					conflict = true;
					break;
				}
			}
		}
		return potentialId;
	}

	protected void ensureAllSatelliteStatus() {
		if (MainProxy.isClient()) {
			return;
		}
		if (satelliteId == 0 && PipeFluidSatellite.AllSatellites.contains(this)) {
			PipeFluidSatellite.AllSatellites.remove(this);
		}
		if (satelliteId != 0 && !PipeFluidSatellite.AllSatellites.contains(this)) {
			PipeFluidSatellite.AllSatellites.add(this);
		}
	}

	public void setNextId(EntityPlayer player) {
		satelliteId = findId(1);
		ensureAllSatelliteStatus();
		if (MainProxy.isClient(player.world)) {
			final ModernPacket packet = PacketHandler.getPacket(SatPipeNext.class).setPosX(getX()).setPosY(getY()).setPosZ(getZ());
			MainProxy.sendPacketToServer(packet);
		} else {
			final ModernPacket packet = PacketHandler.getPacket(SatPipeSetID.class).setSatID(satelliteId).setPosX(getX()).setPosY(getY()).setPosZ(getZ());
			MainProxy.sendPacketToPlayer(packet, player);
		}
		updateWatchers();
	}

	public void setPrevId(EntityPlayer player) {
		satelliteId = findId(-1);
		ensureAllSatelliteStatus();
		if (MainProxy.isClient(player.world)) {
			final ModernPacket packet = PacketHandler.getPacket(SatPipePrev.class).setPosX(getX()).setPosY(getY()).setPosZ(getZ());
			MainProxy.sendPacketToServer(packet);
		} else {
			final ModernPacket packet = PacketHandler.getPacket(SatPipeSetID.class).setSatID(satelliteId).setPosX(getX()).setPosY(getY()).setPosZ(getZ());
			MainProxy.sendPacketToPlayer(packet, player);
		}
		updateWatchers();
	}

	private void updateWatchers() {
		MainProxy.sendToPlayerList(PacketHandler.getPacket(SatPipeSetID.class).setSatID(satelliteId).setPosX(getX()).setPosY(getY()).setPosZ(getZ()), ((PipeFluidSatellite) container.pipe).localModeWatchers);
	}

	@Override
	public void onAllowedRemoval() {
		// TODO PROVIDE REFACTOR: cancel all orders
		if (MainProxy.isClient(getWorld())) {
			return;
		}
		if (PipeFluidSatellite.AllSatellites.contains(this)) {
			PipeFluidSatellite.AllSatellites.remove(this);
		}
	}

	@Override
	public void onWrenchClicked(EntityPlayer entityplayer) {
		// Send the satellite id when opening gui
		final ModernPacket packet = PacketHandler.getPacket(SatPipeSetID.class).setSatID(satelliteId).setPosX(getX()).setPosY(getY()).setPosZ(getZ());
		MainProxy.sendPacketToPlayer(packet, entityplayer);
		entityplayer.openGui(LogisticsPipes.instance, GuiIDs.GUI_SatellitePipe_ID, getWorld(), getX(), getY(), getZ());
	}

	@Override
	public void throttledUpdateEntity() {
		super.throttledUpdateEntity();
		// TODO PROVIDE REFACTOR
	}

	public void setSatelliteId(int integer) {
		satelliteId = integer;
	}

	@Override
	public boolean canReceiveFluid() {
		return false;
	}
}
