package logisticspipes.network.abstractguis;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Container;

public abstract class PopupGuiProvider extends GuiProvider {

	public PopupGuiProvider(int id) {
		super(id);
	}

	@Override
	public final Container getContainer(PlayerEntity player) {
		return null;
	}
}
