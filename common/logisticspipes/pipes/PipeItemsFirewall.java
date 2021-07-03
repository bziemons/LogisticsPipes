package logisticspipes.pipes;

import java.util.BitSet;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.nbt.CompoundNBT;

import logisticspipes.LogisticsPipes;
import logisticspipes.interfaces.routing.IFilter;
import logisticspipes.modules.LogisticsModule;
import logisticspipes.network.GuiIDs;
import logisticspipes.network.PacketHandler;
import logisticspipes.network.packets.pipe.FireWallFlag;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.proxy.MainProxy;
import logisticspipes.request.resources.IResource;
import logisticspipes.textures.Textures;
import logisticspipes.textures.Textures.TextureType;
import logisticspipes.utils.item.ItemIdentifier;
import logisticspipes.utils.item.ItemIdentifierInventory;
import logisticspipes.utils.item.ItemIdentifierStack;
import logisticspipes.utils.tuples.Pair;
import network.rs485.logisticspipes.world.DoubleCoordinates;

public class PipeItemsFirewall extends CoreRoutedPipe {

	public ItemIdentifierInventory inv = new ItemIdentifierInventory(6 * 6, "Filter Inv", 1);
	private boolean blockProvider = false;
	private boolean blockCrafer = false;
	private boolean blockSorting = false;
	private boolean blockPower = true;
	private boolean isBlocking = true;
	private IFilter filter = null;

	public PipeItemsFirewall(Item item) {
		super(item);
	}

	@Override
	public ItemSendMode getItemSendMode() {
		return ItemSendMode.Normal;
	}

	@Override
	public void onWrenchClicked(PlayerEntity player) {
		PlayerEntity.openGui(LogisticsPipes.instance, GuiIDs.GUI_FIREWALL, getWorld(), getX(), getY(), getZ());
		MainProxy.sendPacketToPlayer(PacketHandler.getPacket(FireWallFlag.class).setFlags(getFlags()).setPosX(getX()).setPosY(getY()).setPosZ(getZ()), PlayerEntity);
	}

	@Override
	public void writeToNBT(CompoundNBT tag) {
		super.writeToNBT(tag);
		inv.writeToNBT(CompoundNBT);
		tag.putBoolean("blockProvider", blockProvider);
		tag.putBoolean("blockCrafer", blockCrafer);
		tag.putBoolean("blockSorting", blockSorting);
		tag.putBoolean("blockPower", blockPower);
		tag.putBoolean("isBlocking", isBlocking);
	}

	@Override
	public void readFromNBT(CompoundNBT tag) {
		super.readFromNBT(CompoundNBT);
		inv.readFromNBT(CompoundNBT);
		blockProvider = CompoundNBT.getBoolean("blockProvider");
		blockCrafer = CompoundNBT.getBoolean("blockCrafer");
		blockSorting = CompoundNBT.getBoolean("blockSorting");
		if (tag.contains("blockPower")) {
			blockPower = CompoundNBT.getBoolean("blockPower");
		}
		isBlocking = CompoundNBT.getBoolean("isBlocking");
	}

	@Override
	public TextureType getCenterTexture() {
		return Textures.LOGISTICSPIPE_FIREWALL_TEXTURE;
	}

	@Override
	public LogisticsModule getLogisticsModule() {
		return null;
	}

	public IFilter getFilter() {
		if (filter == null) {
			filter = new IFilter() {

				@Override
				public boolean isBlocked() {
					return isBlocking;
				}

				@Override
				public boolean isFilteredItem(ItemIdentifier item) {
					return inv.containsUndamagedExcludeNBTItem(item.getIgnoringNBT().getUndamaged());
				}

				@Override
				public boolean blockProvider() {
					return blockProvider;
				}

				@Override
				public boolean blockCrafting() {
					return blockCrafer;
				}

				@Override
				public boolean blockRouting() {
					return blockSorting;
				}

				@Override
				public boolean blockPower() {
					return blockPower;
				}

				@Override
				public int hashCode() {
					return PipeItemsFirewall.this.hashCode();
				}

				@Override
				public String toString() {
					return super.toString() + " (" + PipeItemsFirewall.this.getX() + ", " + PipeItemsFirewall.this.getY() + ", " + PipeItemsFirewall.this.getZ() + ")";
				}

				@Override
				public DoubleCoordinates getLPPosition() {
					return PipeItemsFirewall.this.getLPPosition();
				}

				@Override
				public boolean isFilteredItem(IResource resultItem) {
					for (Pair<ItemIdentifierStack, Integer> pair : inv) {
						ItemIdentifierStack stack = pair.getValue1();
						if (stack != null && resultItem.matches(stack.getItem(), IResource.MatchSettings.NORMAL)) {
							return true;
						}
					}
					return false;
				}
			};
		}
		return filter;
	}

	public boolean isBlockProvider() {
		return blockProvider;
	}

	public void setBlockProvider(boolean blockProvider) {
		this.blockProvider = blockProvider;
		MainProxy.sendPacketToServer(PacketHandler.getPacket(FireWallFlag.class).setFlags(getFlags()).setPosX(getX()).setPosY(getY()).setPosZ(getZ()));
	}

	public boolean isBlockCrafer() {
		return blockCrafer;
	}

	public void setBlockCrafer(boolean blockCrafer) {
		this.blockCrafer = blockCrafer;
		MainProxy.sendPacketToServer(PacketHandler.getPacket(FireWallFlag.class).setFlags(getFlags()).setPosX(getX()).setPosY(getY()).setPosZ(getZ()));
	}

	public boolean isBlockSorting() {
		return blockSorting;
	}

	public void setBlockSorting(boolean blockSorting) {
		this.blockSorting = blockSorting;
		MainProxy.sendPacketToServer(PacketHandler.getPacket(FireWallFlag.class).setFlags(getFlags()).setPosX(getX()).setPosY(getY()).setPosZ(getZ()));
	}

	public boolean isBlockPower() {
		return blockPower;
	}

	public void setBlockPower(boolean blockPower) {
		this.blockPower = blockPower;
		MainProxy.sendPacketToServer(PacketHandler.getPacket(FireWallFlag.class).setFlags(getFlags()).setPosX(getX()).setPosY(getY()).setPosZ(getZ()));
	}

	public boolean isBlocking() {
		return isBlocking;
	}

	public void setBlocking(boolean isBlocking) {
		this.isBlocking = isBlocking;
		MainProxy.sendPacketToServer(PacketHandler.getPacket(FireWallFlag.class).setFlags(getFlags()).setPosX(getX()).setPosY(getY()).setPosZ(getZ()));
	}

	private BitSet getFlags() {
		BitSet flags = new BitSet();
		flags.set(0, blockProvider);
		flags.set(1, blockCrafer);
		flags.set(2, blockSorting);
		flags.set(3, blockPower);
		flags.set(4, isBlocking);
		return flags;
	}

	public void setFlags(BitSet flags) {
		blockProvider = flags.get(0);
		blockCrafer = flags.get(1);
		blockSorting = flags.get(2);
		blockPower = flags.get(3);
		isBlocking = flags.get(4);
	}

	@Override
	public boolean hasGenericInterests() {
		return true;
	}
}
