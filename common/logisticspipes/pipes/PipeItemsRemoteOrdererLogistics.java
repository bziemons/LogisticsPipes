package logisticspipes.pipes;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TranslationTextComponent;

import logisticspipes.LPItems;
import logisticspipes.interfaces.routing.IRequestItems;
import logisticspipes.items.RemoteOrderer;
import logisticspipes.modules.LogisticsModule;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.proxy.MainProxy;
import logisticspipes.security.SecuritySettings;
import logisticspipes.textures.Textures;
import logisticspipes.textures.Textures.TextureType;

public class PipeItemsRemoteOrdererLogistics extends CoreRoutedPipe implements IRequestItems {

	public PipeItemsRemoteOrdererLogistics(Item item) {
		super(item);
	}

	@Override
	public TextureType getCenterTexture() {
		return Textures.LOGISTICSPIPE_REMOTE_ORDERER_TEXTURE;
	}

	@Override
	public boolean handleClick(PlayerEntity player, SecuritySettings settings) {
		if (!PlayerEntity.getItemStackFromSlot(EquipmentSlotType.MAINHAND).isEmpty() && PlayerEntity.getItemStackFromSlot(EquipmentSlotType.MAINHAND).getItem() == LPItems.remoteOrderer) {
			if (MainProxy.isServer(getWorld())) {
				if (settings == null || settings.openRequest) {
					ItemStack orderer = PlayerEntity.getItemStackFromSlot(EquipmentSlotType.MAINHAND);
					RemoteOrderer.connectToPipe(orderer, this);
					PlayerEntity.sendMessage(new TranslationTextComponent("lp.chat.connectedtopipe"));
				} else {
					PlayerEntity.sendMessage(new TranslationTextComponent("lp.chat.permissiondenied"));
				}
			}
			return true;
		}
		return false;
	}

	@Override
	public LogisticsModule getLogisticsModule() {
		return null;
	}

	@Override
	public ItemSendMode getItemSendMode() {
		return ItemSendMode.Normal;
	}

}
