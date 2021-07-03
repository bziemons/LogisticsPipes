package logisticspipes.interfaces;

import net.minecraft.entity.player.PlayerEntity;

public interface IModuleWatchReciver {

	void startWatching(PlayerEntity player);

	void stopWatching(PlayerEntity player);
}
