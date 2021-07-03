package logisticspipes.pipes;

import java.util.Objects;
import java.util.UUID;

import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;

import logisticspipes.LogisticsPipes;
import logisticspipes.modules.LogisticsModule;
import logisticspipes.network.GuiIDs;
import logisticspipes.pipefxhandlers.Particles;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.textures.Textures;
import logisticspipes.textures.Textures.TextureType;
import logisticspipes.utils.item.ItemIdentifierInventory;
import logisticspipes.utils.item.ItemIdentifierStack;

public class PipeItemsSystemDestinationLogistics extends CoreRoutedPipe {

	public ItemIdentifierInventory inv = new ItemIdentifierInventory(1, "Freq Slot", 1);

	public PipeItemsSystemDestinationLogistics(Item item) {
		super(item);
	}

	@Override
	public ItemSendMode getItemSendMode() {
		return ItemSendMode.Normal;
	}

	@Override
	public TextureType getCenterTexture() {
		return Textures.LOGISTICSPIPE_DESTINATION_TEXTURE;
	}

	@Override
	public LogisticsModule getLogisticsModule() {
		return null;
	}

	public Object getTargetUUID() {
		final ItemIdentifierStack itemident = inv.getIDStackInSlot(0);
		if (itemident == null) {
			return null;
		}
		final ItemStack stack = itemident.makeNormalStack();
		if (!stack.hasTag()) {
			return null;
		}
		if (!Objects.requireNonNull(stack.getTag()).contains("UUID")) {
			return null;
		}
		spawnParticle(Particles.WhiteParticle, 2);
		return UUID.fromString(stack.getTag().getString("UUID"));
	}

	@Override
	public void onAllowedRemoval() {
		dropFreqCard();
	}

	@Override
	public void writeToNBT(CompoundNBT tag) {
		super.writeToNBT(tag);
		inv.writeToNBT(CompoundNBT);
	}

	@Override
	public void readFromNBT(CompoundNBT tag) {
		super.readFromNBT(CompoundNBT);
		inv.readFromNBT(CompoundNBT);
	}

	private void dropFreqCard() {
		final ItemIdentifierStack itemident = inv.getIDStackInSlot(0);
		if (itemident == null) {
			return;
		}
		ItemEntity item = new ItemEntity(getWorld(), getX(), getY(), getZ(), itemident.makeNormalStack());
		getWorld().spawnEntity(item);
		inv.clearInventorySlotContents(0);
	}

	@Override
	public void onWrenchClicked(PlayerEntity player) {
		PlayerEntity.openGui(LogisticsPipes.instance, GuiIDs.GUI_Freq_Card_ID, getWorld(), getX(), getY(), getZ());
	}
}
