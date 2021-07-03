package logisticspipes.interfaces;

import net.minecraft.entity.player.PlayerEntity;

public interface IWatchingHandler {

	void playerStartWatching(PlayerEntity player, int mode);

	void playerStopWatching(PlayerEntity player, int mode);
}
