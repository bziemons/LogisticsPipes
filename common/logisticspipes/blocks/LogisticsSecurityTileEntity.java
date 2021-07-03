package logisticspipes.blocks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import javax.annotation.Nonnull;

import net.minecraft.crash.CrashReportCategory;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.nbt.IntNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;

import logisticspipes.LPItems;
import logisticspipes.LogisticsPipes;
import logisticspipes.api.IRoutedPowerProvider;
import logisticspipes.interfaces.IGuiOpenControler;
import logisticspipes.interfaces.IGuiTileEntity;
import logisticspipes.interfaces.ISecurityProvider;
import logisticspipes.items.LogisticsItemCard;
import logisticspipes.network.NewGuiHandler;
import logisticspipes.network.PacketHandler;
import logisticspipes.network.abstractguis.CoordinatesGuiProvider;
import logisticspipes.network.guis.block.SecurityStationGui;
import logisticspipes.network.packets.block.SecurityStationAutoDestroy;
import logisticspipes.network.packets.block.SecurityStationCC;
import logisticspipes.network.packets.block.SecurityStationCCIDs;
import logisticspipes.network.packets.block.SecurityStationId;
import logisticspipes.network.packets.block.SecurityStationOpenPlayer;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.proxy.MainProxy;
import logisticspipes.proxy.SimpleServiceLocator;
import logisticspipes.security.SecuritySettings;
import logisticspipes.utils.PlayerCollectionList;
import logisticspipes.utils.item.ItemIdentifierInventory;

public class LogisticsSecurityTileEntity extends LogisticsSolidTileEntity implements IGuiOpenControler, ISecurityProvider, IGuiTileEntity {

	public ItemIdentifierInventory inv = new ItemIdentifierInventory(1, "ID Slots", 64);
	private PlayerCollectionList listener = new PlayerCollectionList();
	private UUID secId = null;
	private Map<ITextComponent, SecuritySettings> settingsList = new HashMap<>();
	public List<Integer> excludedCC = new ArrayList<>();
	public boolean allowCC = false;
	public boolean allowAutoDestroy = false;

	public static PlayerCollectionList byPassed = new PlayerCollectionList();
	public static final SecuritySettings allowAll = new SecuritySettings("");

	static {
		LogisticsSecurityTileEntity.allowAll.openGui = true;
		LogisticsSecurityTileEntity.allowAll.openRequest = true;
		LogisticsSecurityTileEntity.allowAll.openUpgrades = true;
		LogisticsSecurityTileEntity.allowAll.openNetworkMonitor = true;
		LogisticsSecurityTileEntity.allowAll.removePipes = true;
	}

	@Override
	public void invalidate() {
		super.invalidate();
		if (MainProxy.isServer(getWorld())) {
			SimpleServiceLocator.securityStationManager.remove(this);
		}
	}

	@Override
	public void validate() {
		super.validate();
		if (MainProxy.isServer(getWorld())) {
			SimpleServiceLocator.securityStationManager.add(this);
		}
	}

	@Override
	public void onChunkUnloaded() {
		super.onChunkUnloaded();
		if (MainProxy.isServer(getWorld())) {
			SimpleServiceLocator.securityStationManager.remove(this);
		}
	}

	public void deauthorizeStation() {
		SimpleServiceLocator.securityStationManager.deauthorizeUUID(getSecId());
	}

	public void authorizeStation() {
		SimpleServiceLocator.securityStationManager.authorizeUUID(getSecId());
	}

	@Override
	public void guiOpenedByPlayer(PlayerEntity player) {
		MainProxy.sendPacketToPlayer(PacketHandler.getPacket(SecurityStationCC.class).setInteger(allowCC ? 1 : 0).setBlockPos(pos), player);
		MainProxy.sendPacketToPlayer(PacketHandler.getPacket(SecurityStationAutoDestroy.class).setInteger(allowAutoDestroy ? 1 : 0).setBlockPos(pos), player);
		MainProxy.sendPacketToPlayer(PacketHandler.getPacket(SecurityStationId.class).setUuid(getSecId()).setBlockPos(pos), player);
		SimpleServiceLocator.securityStationManager.sendClientAuthorizationList();
		listener.add(player);
	}

	@Override
	public void guiClosedByPlayer(PlayerEntity player) {
		listener.remove(player);
	}

	public UUID getSecId() {
		if (MainProxy.isServer(getWorld())) {
			if (secId == null) {
				secId = UUID.randomUUID();
			}
		}
		return secId;
	}

	public CompoundNBT getSecTag() {
		final CompoundNBT tag = new CompoundNBT();
		tag.putString("UUID", getSecId().toString());
		tag.putInt("Type", LogisticsItemCard.SEC_CARD);
		return tag;
	}

	public void setClientUUID(UUID id) {
		if (MainProxy.isClient(getWorld())) {
			secId = id;
		}
	}

	public void setClientCC(boolean flag) {
		if (MainProxy.isClient(getWorld())) {
			allowCC = flag;
		}
	}

	public void setClientDestroy(boolean flag) {
		if (MainProxy.isClient(getWorld())) {
			allowAutoDestroy = flag;
		}
	}

	@Override
	public void readFromNBT(CompoundNBT tag) {
		super.readFromNBT(tag);
		if (tag.contains("UUID")) {
			secId = UUID.fromString(tag.getString("UUID"));
		}
		allowCC = tag.getBoolean("allowCC");
		allowAutoDestroy = tag.getBoolean("allowAutoDestroy");
		inv.readFromNBT(tag);
		settingsList.clear();
		ListNBT list = tag.getList("settings", 10);
		while (list.size() > 0) {
			INBT base = list.remove(0);
			String name = ((CompoundNBT) base).getString("name");
			CompoundNBT value = ((CompoundNBT) base).getCompound("content");
			SecuritySettings settings = new SecuritySettings(name);
			settings.readFromNBT(value);
			settingsList.put(name, settings);
		}
		excludedCC.clear();
		list = tag.getList("excludedCC", 3);
		while (list.size() > 0) {
			INBT base = list.remove(0);
			excludedCC.add(((IntNBT) base).getInt());
		}
	}

	@Override
	public CompoundNBT writeToNBT(CompoundNBT tag) {
		tag = super.writeToNBT(tag);
		tag.putString("UUID", getSecId().toString());
		tag.putBoolean("allowCC", allowCC);
		tag.putBoolean("allowAutoDestroy", allowAutoDestroy);
		inv.writeToNBT(tag);
		ListNBT settingsList = new ListNBT();
		for (Entry<String, SecuritySettings> entry : this.settingsList.entrySet()) {
			CompoundNBT settingTag = new CompoundNBT();
			settingTag.putString("name", entry.getKey());
			CompoundNBT value = new CompoundNBT();
			entry.getValue().writeToNBT(value);
			settingTag.put("content", value);
			settingsList.add(settingTag);
		}
		tag.put("settings", settingsList);
		settingsList = new ListNBT();
		for (Integer i : excludedCC) {
			settingsList.add(new IntNBT(i));
		}
		tag.put("excludedCC", settingsList);
		return tag;
	}

	public void buttonFreqCard(int integer, PlayerEntity player) {
		switch (integer) {
			case 0: //--
				inv.clearInventorySlotContents(0);
				break;
			case 1: //-
				inv.decrStackSize(0, 1);
				break;
			case 2: //+
				if (!useEnergy(10)) {
					player.sendMessage(new TranslationTextComponent("lp.misc.noenergy"));
					return;
				}
				if (inv.getIDStackInSlot(0) == null) {
					ItemStack stack = new ItemStack(LPItems.itemCard, 1, getSecTag());
					inv.setInventorySlotContents(0, stack);
				} else {
					ItemStack slot = inv.getStackInSlot(0);
					if (slot.getCount() < 64) {
						slot.grow(1);
						slot.setTag(getSecTag());
						inv.setInventorySlotContents(0, slot);
					}
				}
				break;
			case 3: //++
				if (!useEnergy(640)) {
					player.sendMessage(new TranslationTextComponent("lp.misc.noenergy"));
					return;
				}
				ItemStack stack = new ItemStack(LPItems.itemCard, 64, getSecTag());
				inv.setInventorySlotContents(0, stack);
				break;
		}
	}

	public void handleOpenSecurityPlayer(PlayerEntity player, @Nonnull String string) {
		SecuritySettings setting = settingsList.get(string);
		if (setting == null) {
			if (string.isEmpty()) return;
			setting = new SecuritySettings(string);
			settingsList.put(string, setting);
		}
		CompoundNBT nbt = new CompoundNBT();
		setting.writeToNBT(nbt);
		MainProxy.sendPacketToPlayer(PacketHandler.getPacket(SecurityStationOpenPlayer.class).setTag(nbt), player);
	}

	public void saveNewSecuritySettings(CompoundNBT tag) {
		SecuritySettings setting = settingsList.get(tag.getString("name"));
		if (setting == null) {
			setting = new SecuritySettings(tag.getString("name"));
			settingsList.put(tag.getString("name"), setting);
		}
		setting.readFromNBT(tag);
	}

	public SecuritySettings getSecuritySettingsForPlayer(PlayerEntity player, boolean usePower) {
		if (LogisticsSecurityTileEntity.byPassed.contains(player)) {
			return LogisticsSecurityTileEntity.allowAll;
		}
		if (usePower && !useEnergy(10)) {
			player.sendMessage(new TranslationTextComponent("lp.misc.noenergy"));
			return new SecuritySettings("No Energy");
		}
		SecuritySettings setting = settingsList.get(player.getDisplayName());
		//TODO Change to GameProfile based Authentication
		if (setting == null) {
			setting = new SecuritySettings(player.getDisplayNameString());
			settingsList.put(player.getDisplayNameString(), setting);
		}
		return setting;
	}

	public void changeCC() {
		allowCC = !allowCC;
		MainProxy.sendToPlayerList(PacketHandler.getPacket(SecurityStationCC.class).setInteger(allowCC ? 1 : 0).setBlockPos(pos), listener);
	}

	public void changeDestroy() {
		allowAutoDestroy = !allowAutoDestroy;
		MainProxy.sendToPlayerList(PacketHandler.getPacket(SecurityStationAutoDestroy.class).setInteger(allowAutoDestroy ? 1 : 0).setBlockPos(pos), listener);
	}

	public void addCCToList(Integer id) {
		if (!excludedCC.contains(id)) {
			excludedCC.add(id);
		}
		Collections.sort(excludedCC);
	}

	public void removeCCFromList(Integer id) {
		excludedCC.remove(id);
	}

	public void requestList(PlayerEntity player) {
		CompoundNBT tag = new CompoundNBT();
		ListNBT list = new ListNBT();
		for (Integer i : excludedCC) {
			list.add(new IntNBT(i));
		}
		tag.setTag("list", list);
		MainProxy.sendPacketToPlayer(PacketHandler.getPacket(SecurityStationCCIDs.class).setTag(tag).setBlockPos(pos), player);
	}

	public void handleListPacket(CompoundNBT tag) {
		excludedCC.clear();
		ListNBT list = tag.getList("list", 3);
		while (list.size() > 0) {
			INBT base = list.remove(0);
			excludedCC.add(((IntNBT) base).getInt());
		}
	}

	@Override
	public boolean getAllowCC(int id) {
		if (!useEnergy(10)) {
			return false;
		}
		return allowCC != excludedCC.contains(id);
	}

	@Override
	public boolean canAutomatedDestroy() {
		if (!useEnergy(10)) {
			return false;
		}
		return allowAutoDestroy;
	}

	private boolean useEnergy(int amount) {
		for (int i = 0; i < 4; i++) {
			TileEntity tile = getWorld().getTileEntity(getPos().offset(Direction.values()[i + 2]));
			if (tile instanceof IRoutedPowerProvider) {
				if (((IRoutedPowerProvider) tile).useEnergy(amount)) {
					return true;
				}
			}
			if (tile instanceof LogisticsTileGenericPipe) {
				if (((LogisticsTileGenericPipe) tile).pipe instanceof IRoutedPowerProvider) {
					if (((IRoutedPowerProvider) ((LogisticsTileGenericPipe) tile).pipe).useEnergy(amount)) {
						return true;
					}
				}
			}
		}
		return false;
	}

	@Override
	public void addInfoToCrashReport(CrashReportCategory par1CrashReportCategory) {
		super.addInfoToCrashReport(par1CrashReportCategory);
		par1CrashReportCategory.func_71507_a("LP-Version", LogisticsPipes.getVersionString());
	}

	@Override
	public CoordinatesGuiProvider getGuiProvider() {
		return NewGuiHandler.getGui(SecurityStationGui.class);
	}
}
