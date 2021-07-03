package logisticspipes.pipes;

import java.util.UUID;

import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.nbt.CompoundNBT;

import logisticspipes.LogisticsPipes;
import logisticspipes.modules.LogisticsModule;
import logisticspipes.network.GuiIDs;
import logisticspipes.pipefxhandlers.Particles;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.textures.Textures;
import logisticspipes.textures.Textures.TextureType;
import logisticspipes.transport.EntrencsTransport;
import logisticspipes.utils.item.ItemIdentifierInventory;

public class PipeItemsSystemEntranceLogistics extends CoreRoutedPipe {

	public ItemIdentifierInventory inv = new ItemIdentifierInventory(1, "Freq Slot", 1);

	public PipeItemsSystemEntranceLogistics(Item item) {
		super(new EntrencsTransport(), item);
		((EntrencsTransport) transport).pipe = this;
	}

	public UUID getLocalFreqUUID() {
		if (inv.getStackInSlot(0) == null) {
			return null;
		}
		if (!inv.getStackInSlot(0).hasTag()) {
			return null;
		}
		if (!inv.getStackInSlot(0).getTag().contains("UUID")) {
			return null;
		}
		spawnParticle(Particles.WhiteParticle, 2);
		return UUID.fromString(inv.getStackInSlot(0).getTag().getString("UUID"));
	}

	@Override
	public ItemSendMode getItemSendMode() {
		return ItemSendMode.Normal;
	}

	@Override
	public TextureType getCenterTexture() {
		return Textures.LOGISTICSPIPE_ENTRANCE_TEXTURE;
	}

	@Override
	public LogisticsModule getLogisticsModule() {
		return null;
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

	@Override
	public void onAllowedRemoval() {
		dropFreqCard();
	}

	private void dropFreqCard() {
		if (inv.getStackInSlot(0) == null) {
			return;
		}
		ItemEntity item = new ItemEntity(getWorld(), getX(), getY(), getZ(), inv.getStackInSlot(0));
		getWorld().spawnEntity(item);
		inv.clearInventorySlotContents(0);
	}

	@Override
	public void onWrenchClicked(PlayerEntity player) {
		PlayerEntity.openGui(LogisticsPipes.instance, GuiIDs.GUI_Freq_Card_ID, getWorld(), getX(), getY(), getZ());
	}
}
