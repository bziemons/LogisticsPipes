package logisticspipes.pipes;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.nbt.NBTTagCompound;

import lombok.Getter;

import logisticspipes.LogisticsPipes;
import logisticspipes.network.GuiIDs;
import logisticspipes.network.PacketHandler;
import logisticspipes.network.packets.pipe.FluidSupplierAmount;
import logisticspipes.pipes.basic.fluid.FluidRoutedPipe;
import logisticspipes.proxy.MainProxy;
import logisticspipes.textures.Textures;
import logisticspipes.textures.Textures.TextureType;
import logisticspipes.utils.FluidIdentifier;
import logisticspipes.utils.item.ItemIdentifierInventory;

public class PipeFluidSupplierMk2 extends FluidRoutedPipe {

	public enum MinMode {
		NONE(0),
		ONEBUCKET(1000),
		TWOBUCKET(2000),
		FIVEBUCKET(5000);

		@Getter
		private final int amount;

		MinMode(int amount) {
			this.amount = amount;
		}
	}

	public PipeFluidSupplierMk2(Item item) {
		super(item);
		throttleTime = 100;
	}

	@Override
	public ItemSendMode getItemSendMode() {
		return ItemSendMode.Fast;
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
		return Textures.LOGISTICSPIPE_LIQUIDSUPPLIER_MK2_TEXTURE;
	}

	@Override
	public boolean hasGenericInterests() {
		return true;
	}

	//from PipeFluidSupplierMk2
	private ItemIdentifierInventory dummyInventory = new ItemIdentifierInventory(1, "Fluid to keep stocked", 127, true);
	private int amount = 0;

	private final Map<FluidIdentifier, Integer> _requestedItems = new HashMap<>();

	private boolean _requestPartials = false;
	private MinMode _bucketMinimum = MinMode.ONEBUCKET;

	@Override
	public void throttledUpdateEntity() {
		if (!isEnabled()) {
			return;
		}
		if (MainProxy.isClient(container.getWorld())) {
			return;
		}
		super.throttledUpdateEntity();
		if (dummyInventory.getStackInSlot(0) == null) {
			return;
		}

		// TODO PROVIDE REFACTOR: request available fluid
	}

	@Override
	public void readFromNBT(NBTTagCompound nbttagcompound) {
		super.readFromNBT(nbttagcompound);
		dummyInventory.readFromNBT(nbttagcompound, "");
		_requestPartials = nbttagcompound.getBoolean("requestpartials");
		amount = nbttagcompound.getInteger("amount");
		_bucketMinimum = MinMode.values()[nbttagcompound.getByte("_bucketMinimum")];
	}

	@Override
	public void writeToNBT(NBTTagCompound nbttagcompound) {
		super.writeToNBT(nbttagcompound);
		dummyInventory.writeToNBT(nbttagcompound, "");
		nbttagcompound.setBoolean("requestpartials", _requestPartials);
		nbttagcompound.setInteger("amount", amount);
		nbttagcompound.setByte("_bucketMinimum", (byte) _bucketMinimum.ordinal());
	}

	private void decreaseRequested(FluidIdentifier liquid, int remaining) {
		//see if we can get an exact match
		Integer count = _requestedItems.get(liquid);
		if (count != null) {
			_requestedItems.put(liquid, Math.max(0, count - remaining));
			remaining -= count;
		}
		if (remaining <= 0) {
			return;
		}
		//still remaining... was from fuzzyMatch on a crafter
		for (Entry<FluidIdentifier, Integer> e : _requestedItems.entrySet()) {
			if (e.getKey().equals(liquid)) {
				int expected = e.getValue();
				e.setValue(Math.max(0, expected - remaining));
				remaining -= expected;
			}
			if (remaining <= 0) {
				return;
			}
		}
		//we have no idea what this is, log it.
		debug.log("liquid supplier got unexpected item " + liquid.toString());
	}

	public boolean isRequestingPartials() {
		return _requestPartials;
	}

	public void setRequestingPartials(boolean value) {
		_requestPartials = value;
	}

	public MinMode getMinMode() {
		return _bucketMinimum;
	}

	public void setMinMode(MinMode value) {
		_bucketMinimum = value;
	}

	@Override
	public void onWrenchClicked(EntityPlayer entityplayer) {
		entityplayer.openGui(LogisticsPipes.instance, GuiIDs.GUI_FluidSupplier_MK2_ID, getWorld(), getX(), getY(), getZ());
	}

	public IInventory getDummyInventory() {
		return dummyInventory;
	}

	public int getAmount() {
		return amount;
	}

	public void setAmount(int amount) {
		if (MainProxy.isClient(container.getWorld())) {
			this.amount = amount;
		}
	}

	public void changeFluidAmount(int change, EntityPlayer player) {
		amount += change;
		if (amount <= 0) {
			amount = 0;
		}
		MainProxy.sendPacketToPlayer(PacketHandler.getPacket(FluidSupplierAmount.class).setInteger(amount).setPosX(getX()).setPosY(getY()).setPosZ(getZ()), player);
	}

	@Override
	public boolean canReceiveFluid() {
		return false;
	}
}
