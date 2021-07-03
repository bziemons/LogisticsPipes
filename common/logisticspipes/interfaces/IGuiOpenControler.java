package logisticspipes.interfaces;

import net.minecraft.entity.player.PlayerEntity;

public interface IGuiOpenControler {

	void guiOpenedByPlayer(PlayerEntity player);

	void guiClosedByPlayer(PlayerEntity player);
}
