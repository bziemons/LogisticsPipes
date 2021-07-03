package logisticspipes.pipes.upgrades;

import java.util.EnumSet;
import java.util.Objects;
import java.util.UUID;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.Direction;
import net.minecraft.world.World;

import logisticspipes.LPItems;
import logisticspipes.interfaces.IGuiOpenControler;
import logisticspipes.interfaces.IPipeUpgradeManager;
import logisticspipes.interfaces.ISlotUpgradeManager;
import logisticspipes.items.ItemUpgrade;
import logisticspipes.items.LogisticsItemCard;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.pipes.upgrades.power.BCPowerSupplierUpgrade;
import logisticspipes.pipes.upgrades.power.IC2PowerSupplierUpgrade;
import logisticspipes.pipes.upgrades.power.RFPowerSupplierUpgrade;
import logisticspipes.proxy.MainProxy;
import logisticspipes.utils.ISimpleInventoryEventHandler;
import logisticspipes.utils.PlayerCollectionList;
import logisticspipes.utils.item.ItemIdentifier;
import logisticspipes.utils.item.SimpleStackInventory;
import network.rs485.logisticspipes.world.DoubleCoordinates;

public class UpgradeManager implements ISimpleInventoryEventHandler, ISlotUpgradeManager, IPipeUpgradeManager {

	public final SimpleStackInventory inv = new SimpleStackInventory(9, "UpgradeInventory", 16);
	public final SimpleStackInventory sneakyInv = new SimpleStackInventory(9, "SneakyUpgradeInventory", 1);
	public final SimpleStackInventory secInv = new SimpleStackInventory(1, "SecurityInventory", 16);
	private IPipeUpgrade[] upgrades = new IPipeUpgrade[9];
	private IPipeUpgrade[] sneakyUpgrades = new IPipeUpgrade[9];
	private CoreRoutedPipe pipe;
	private int securityDelay = 0;

	/* cached attributes */
	private Direction sneakyOrientation = null;
	private Direction[] combinedSneakyOrientation = new Direction[9];
	private int speedUpgradeCount = 0;
	private final EnumSet<Direction> disconnectedSides = EnumSet.noneOf(Direction.class);
	private boolean isAdvancedCrafter = false;
	private boolean isFuzzyUpgrade = false;
	private boolean isCombinedSneakyUpgrade = false;
	private int liquidCrafter = 0;
	private boolean hasByproductExtractor = false;
	private UUID uuid = null;
	private String uuidS = null;
	private boolean hasPatternUpgrade = false;
	private boolean hasPowerPassUpgrade = false;
	private boolean hasRFPowerUpgrade = false;
	private boolean hasBCPowerUpgrade = false;
	private int getIC2PowerLevel = 0;
	private boolean hasCCRemoteControlUpgrade = false;
	private boolean hasCraftingMonitoringUpgrade = false;
	private boolean hasOpaqueUpgrade = false;
	private int craftingCleanup = 0;
	private boolean hasLogicControll = false;
	private boolean hasUpgradeModuleUpgarde = false;
	private int actionSpeedUpgrade = 0;
	private int itemExtractionUpgrade = 0;
	private int itemStackExtractionUpgrade = 0;

	private boolean[] guiUpgrades = new boolean[18];

	private boolean needsContainerPositionUpdate = false;

	public UpgradeManager(CoreRoutedPipe pipe) {
		this.pipe = pipe;
		inv.addListener(this);
		sneakyInv.addListener(this);
		secInv.addListener(this);
	}

	public void readFromNBT(CompoundNBT tag) {
		inv.readFromNBT(tag, "UpgradeInventory_");
		sneakyInv.readFromNBT(tag, "SneakyUpgradeInventory_");
		secInv.readFromNBT(tag, "SecurityInventory_");

		if (!sneakyInv.getStackInSlot(8).isEmpty()) {
			if (sneakyInv.getStackInSlot(8).getItem() == LPItems.itemCard && sneakyInv.getStackInSlot(8).getDamage() == LogisticsItemCard.SEC_CARD) {
				secInv.setInventorySlotContents(0, sneakyInv.getStackInSlot(8));
				sneakyInv.setInventorySlotContents(8, ItemStack.EMPTY);
			}
		}

		InventoryChanged(inv);
	}

	public void writeToNBT(CompoundNBT tag) {
		inv.writeToNBT(tag, "UpgradeInventory_");
		sneakyInv.writeToNBT(tag, "SneakyUpgradeInventory_");
		secInv.writeToNBT(tag, "SecurityInventory_");
		InventoryChanged(inv);
	}

	private boolean updateModule(int slot, IPipeUpgrade[] upgrades, IInventory inv) {
		ItemStack stack = inv.getStackInSlot(slot);
		if (stack.getItem() instanceof ItemUpgrade) {
			upgrades[slot] = ((ItemUpgrade) stack.getItem()).getUpgradeForItem(stack, upgrades[slot]);
		} else {
			upgrades[slot] = null;
		}
		if (upgrades[slot] == null) {
			inv.setInventorySlotContents(slot, ItemStack.EMPTY);
			return false;
		} else {
			return upgrades[slot].needsUpdate();
		}
	}

	private boolean removeUpgrade(int slot, IPipeUpgrade[] upgrades) {
		boolean needUpdate = upgrades[slot].needsUpdate();
		upgrades[slot] = null;
		return needUpdate;
	}

	@Override
	public void InventoryChanged(IInventory inventory) {
		boolean needUpdate = false;
		for (int i = 0; i < inv.getSizeInventory(); i++) {
			ItemStack item = inv.getStackInSlot(i);
			if (!item.isEmpty()) {
				needUpdate |= updateModule(i, upgrades, inv);
			} else if (upgrades[i] != null) {
				needUpdate |= removeUpgrade(i, upgrades);
			}
		}
		//update sneaky direction, speed upgrade count and disconnection
		sneakyOrientation = null;
		speedUpgradeCount = 0;
		isAdvancedCrafter = false;
		isFuzzyUpgrade = false;
		boolean combinedBuffer = isCombinedSneakyUpgrade;
		isCombinedSneakyUpgrade = false;
		liquidCrafter = 0;
		disconnectedSides.clear();
		hasByproductExtractor = false;
		hasPatternUpgrade = false;
		hasPowerPassUpgrade = false;
		hasRFPowerUpgrade = false;
		hasBCPowerUpgrade = false;
		getIC2PowerLevel = 0;
		hasCCRemoteControlUpgrade = false;
		hasCraftingMonitoringUpgrade = false;
		hasOpaqueUpgrade = false;
		craftingCleanup = 0;
		hasLogicControll = false;
		hasUpgradeModuleUpgarde = false;
		actionSpeedUpgrade = 0;
		itemExtractionUpgrade = 0;
		itemStackExtractionUpgrade = 0;

		guiUpgrades = new boolean[18];
		for (int i = 0; i < upgrades.length; i++) {
			IPipeUpgrade upgrade = upgrades[i];
			if (upgrade instanceof SneakyUpgradeConfig && sneakyOrientation == null && !isCombinedSneakyUpgrade) {
				sneakyOrientation = ((SneakyUpgradeConfig) upgrade).getSide(getInv().getStackInSlot(i));
			} else if (upgrade instanceof SpeedUpgrade) {
				speedUpgradeCount += inv.getStackInSlot(i).getCount();
			} else if (upgrade instanceof ConnectionUpgradeConfig) {
				((ConnectionUpgradeConfig) upgrade).getSides(getInv().getStackInSlot(i)).forEach(disconnectedSides::add);
			} else if (upgrade instanceof AdvancedSatelliteUpgrade) {
				isAdvancedCrafter = true;
			} else if (upgrade instanceof FuzzyUpgrade) {
				isFuzzyUpgrade = true;
			} else if (upgrade instanceof CombinedSneakyUpgrade && sneakyOrientation == null) {
				isCombinedSneakyUpgrade = true;
			} else if (upgrade instanceof FluidCraftingUpgrade) {
				liquidCrafter += inv.getStackInSlot(i).getCount();
			} else if (upgrade instanceof CraftingByproductUpgrade) {
				hasByproductExtractor = true;
			} else if (upgrade instanceof PatternUpgrade) {
				hasPatternUpgrade = true;
			} else if (upgrade instanceof PowerTransportationUpgrade) {
				hasPowerPassUpgrade = true;
			} else if (upgrade instanceof RFPowerSupplierUpgrade) {
				hasRFPowerUpgrade = true;
			} else if (upgrade instanceof BCPowerSupplierUpgrade) {
				hasBCPowerUpgrade = true;
			} else if (upgrade instanceof IC2PowerSupplierUpgrade) {
				getIC2PowerLevel = Math.max(getIC2PowerLevel, ((IC2PowerSupplierUpgrade) upgrade).getPowerLevel());
			} else if (upgrade instanceof CCRemoteControlUpgrade) {
				hasCCRemoteControlUpgrade = true;
			} else if (upgrade instanceof CraftingMonitoringUpgrade) {
				hasCraftingMonitoringUpgrade = true;
			} else if (upgrade instanceof OpaqueUpgrade) {
				hasOpaqueUpgrade = true;
			} else if (upgrade instanceof CraftingCleanupUpgrade) {
				craftingCleanup += inv.getStackInSlot(i).getCount();
			} else if (upgrade instanceof LogicControllerUpgrade) {
				hasLogicControll = true;
			} else if (upgrade instanceof UpgradeModuleUpgrade) {
				hasUpgradeModuleUpgarde = true;
			} else if (upgrade instanceof ActionSpeedUpgrade) {
				actionSpeedUpgrade += inv.getStackInSlot(i).getCount();
			} else if (upgrade instanceof ItemExtractionUpgrade) {
				itemExtractionUpgrade += inv.getStackInSlot(i).getCount();
			} else if (upgrade instanceof ItemStackExtractionUpgrade) {
				itemStackExtractionUpgrade += inv.getStackInSlot(i).getCount();
			}
			if (upgrade instanceof IConfigPipeUpgrade) {
				guiUpgrades[i] = true;
			}
		}
		liquidCrafter = Math.min(liquidCrafter, ItemUpgrade.MAX_LIQUID_CRAFTER);
		craftingCleanup = Math.min(craftingCleanup, ItemUpgrade.MAX_CRAFTING_CLEANUP);
		itemExtractionUpgrade = Math.min(itemExtractionUpgrade, ItemUpgrade.MAX_ITEM_EXTRACTION);
		itemStackExtractionUpgrade = Math.min(itemStackExtractionUpgrade, ItemUpgrade.MAX_ITEM_STACK_EXTRACTION);
		if (combinedBuffer != isCombinedSneakyUpgrade) {
			needsContainerPositionUpdate = true;
		}
		for (int i = 0; i < sneakyInv.getSizeInventory(); i++) {
			ItemStack item = sneakyInv.getStackInSlot(i);
			if (!item.isEmpty()) {
				needUpdate |= updateModule(i, sneakyUpgrades, sneakyInv);
			} else if (sneakyUpgrades[i] != null) {
				needUpdate |= removeUpgrade(i, sneakyUpgrades);
			}
		}
		for (int i = 0; i < sneakyUpgrades.length; i++) {
			IPipeUpgrade upgrade = sneakyUpgrades[i];
			if (upgrade instanceof SneakyUpgradeConfig) {
				ItemStack stack = sneakyInv.getStackInSlot(i);
				combinedSneakyOrientation[i] = ((SneakyUpgradeConfig) upgrade).getSide(stack);
			}
			if (upgrade instanceof IConfigPipeUpgrade) {
				guiUpgrades[i + 9] = true;
			}
		}
		if (needUpdate) {
			MainProxy.runOnServer(null, () -> () -> {
				pipe.connectionUpdate();
				if (pipe.container != null) {
					pipe.container.sendUpdateToClient();
				}
			});
		}
		uuid = null;
		uuidS = null;
		ItemStack stack = secInv.getStackInSlot(0);
		if (stack.isEmpty()) {
			return;
		}
		if (stack.getItem() != LPItems.itemCard || stack.getDamage() != LogisticsItemCard.SEC_CARD) {
			return;
		}
		if (!stack.hasTag()) {
			return;
		}
		if (!stack.getTag().contains("UUID")) {
			return;
		}
		uuid = UUID.fromString(stack.getTag().getString("UUID"));
		uuidS = uuid.toString();
	}

	/* Special implementations */

	@Override
	public boolean hasSneakyUpgrade() {
		return sneakyOrientation != null;
	}

	@Override
	public Direction getSneakyOrientation() {
		return sneakyOrientation;
	}

	@Override
	public int getSpeedUpgradeCount() {
		return speedUpgradeCount;
	}

	@Override
	public boolean hasCombinedSneakyUpgrade() {
		return isCombinedSneakyUpgrade;
	}

	@Override
	public Direction[] getCombinedSneakyOrientation() {
		return combinedSneakyOrientation;
	}

	public IGuiOpenControler getGuiController() {
		return new IGuiOpenControler() {

			PlayerCollectionList players = new PlayerCollectionList();

			@Override
			public void guiOpenedByPlayer(PlayerEntity player) {
				players.add(player);
			}

			@Override
			public void guiClosedByPlayer(PlayerEntity player) {
				players.remove(player);
				if (players.isEmpty() && !isCombinedSneakyUpgrade) {
					sneakyInv.dropContents(pipe.getWorld(), pipe.getPos());
				}
			}
		};
	}

	public boolean isNeedingContainerUpdate() {
		boolean tmp = needsContainerPositionUpdate;
		needsContainerPositionUpdate = false;
		return tmp;
	}

	public void dropUpgrades() {
		inv.dropContents(pipe.getWorld(), pipe.getPos());
		sneakyInv.dropContents(pipe.getWorld(), pipe.getPos());
	}

	@Override
	public boolean isSideDisconnected(Direction side) {
		return disconnectedSides.contains(side);
	}

	public boolean tryIserting(World world, PlayerEntity player) {
		ItemStack itemStackInMainHand = PlayerEntity.getItemStackFromSlot(EquipmentSlotType.MAINHAND);
		if (!itemStackInMainHand.isEmpty() && itemStackInMainHand.getItem() instanceof ItemUpgrade) {
			if (MainProxy.isClient(world)) {
				return true;
			}
			IPipeUpgrade upgrade = ((ItemUpgrade) itemStackInMainHand.getItem()).getUpgradeForItem(itemStackInMainHand, null);
			if (upgrade.isAllowedForPipe(pipe)) {
				if (isCombinedSneakyUpgrade) {
					if (upgrade instanceof SneakyUpgradeConfig) {
						if (insertIntInv(PlayerEntity, sneakyInv)) {
							return true;
						}
					}
				}
				if (insertIntInv(PlayerEntity, inv)) {
					return true;
				}
			}
		}
		if (!itemStackInMainHand.isEmpty() && itemStackInMainHand.getItem() == LPItems.itemCard && itemStackInMainHand.getDamage() == LogisticsItemCard.SEC_CARD) {
			if (MainProxy.isClient(world)) {
				return true;
			}
			if (secInv.getStackInSlot(0).isEmpty()) {
				ItemStack newItem = itemStackInMainHand.split(1);
				secInv.setInventorySlotContents(0, newItem);
				InventoryChanged(secInv);
				return true;
			}
		}
		return false;
	}

	private boolean insertIntInv(PlayerEntity player, SimpleStackInventory inv) {
		for (int i = 0; i < inv.getSizeInventory(); i++) {
			ItemStack item = inv.getStackInSlot(i);
			if (item.isEmpty()) {
				inv.setInventorySlotContents(i, PlayerEntity.getItemStackFromSlot(EquipmentSlotType.MAINHAND).split(1));
				InventoryChanged(inv);
				return true;
			} else if (ItemIdentifier.get(item).equals(ItemIdentifier.get(PlayerEntity.getItemStackFromSlot(EquipmentSlotType.MAINHAND)))) {
				if (item.getCount() < inv.getInventoryStackLimit()) {
					item.grow(1);
					PlayerEntity.getItemStackFromSlot(EquipmentSlotType.MAINHAND).split(1);
					inv.setInventorySlotContents(i, item);
					InventoryChanged(inv);
					return true;
				}
			}
		}
		return false;
	}

	public UUID getSecurityID() {
		return uuid;
	}

	public void insetSecurityID(UUID id) {
		ItemStack stack = new ItemStack(LPItems.itemCard, 1, LogisticsItemCard.SEC_CARD);
		stack.setTag(new CompoundNBT());
		final CompoundNBT tag = Objects.requireNonNull(stack.getTag());
		tag.putString("UUID", id.toString());
		secInv.setInventorySlotContents(0, stack);
		InventoryChanged(secInv);
	}

	public void securityTick() {
		if ((getSecurityID()) != null) {
			if (!SimpleServiceLocator.securityStationManager.isAuthorized(uuidS)) {
				securityDelay++;
			} else {
				securityDelay = 0;
			}
			if (securityDelay > 20) {
				secInv.clearInventorySlotContents(0);
				InventoryChanged(secInv);
			}
		}
	}

	@Override
	public boolean isAdvancedSatelliteCrafter() {
		return isAdvancedCrafter;
	}

	@Override
	public boolean isFuzzyUpgrade() {
		return isFuzzyUpgrade;
	}

	@Override
	public int getFluidCrafter() {
		return liquidCrafter;
	}

	@Override
	public boolean hasByproductExtractor() {
		return hasByproductExtractor;
	}

	@Override
	public boolean hasPatternUpgrade() {
		return hasPatternUpgrade;
	}

	@Override
	public boolean hasPowerPassUpgrade() {
		return hasPowerPassUpgrade || hasRFPowerUpgrade || hasBCPowerUpgrade || getIC2PowerLevel > 0;
	}

	@Override
	public boolean hasRFPowerSupplierUpgrade() {
		return hasRFPowerUpgrade;
	}

	@Override
	public boolean hasBCPowerSupplierUpgrade() {
		return hasBCPowerUpgrade;
	}

	@Override
	public int getIC2PowerLevel() {
		return getIC2PowerLevel;
	}

	@Override
	public boolean hasCCRemoteControlUpgrade() {
		return hasCCRemoteControlUpgrade;
	}

	@Override
	public boolean hasCraftingMonitoringUpgrade() {
		return hasCraftingMonitoringUpgrade;
	}

	@Override
	public boolean isOpaque() {
		return hasOpaqueUpgrade;
	}

	@Override
	public int getCrafterCleanup() {
		return craftingCleanup;
	}

	public boolean hasLogicControll() {
		return hasLogicControll;
	}

	@Override
	public boolean hasUpgradeModuleUpgrade() {
		return hasUpgradeModuleUpgarde;
	}

	@Override
	public boolean hasOwnSneakyUpgrade() {
		return false;
	}

	public boolean hasGuiUpgrade(int i) {
		return guiUpgrades[i];
	}

	public IPipeUpgrade getUpgrade(int i) {
		if (i < upgrades.length) {
			return upgrades[i];
		} else {
			return sneakyUpgrades[i - upgrades.length];
		}
	}

	@Override
	public DoubleCoordinates getPipePosition() {
		return pipe.getLPPosition();
	}

	@Override
	public int getActionSpeedUpgrade() {
		return actionSpeedUpgrade;
	}

	@Override
	public int getItemExtractionUpgrade() {
		return itemExtractionUpgrade;
	}

	@Override
	public int getItemStackExtractionUpgrade() {
		return itemStackExtractionUpgrade;
	}

	@Override
	public SimpleStackInventory getInv() {
		return this.inv;
	}

}
