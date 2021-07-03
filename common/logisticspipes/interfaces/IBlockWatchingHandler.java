package logisticspipes.interfaces;

import net.minecraft.entity.player.PlayerEntity;

public interface IBlockWatchingHandler {

	void playerStartWatching(PlayerEntity player);

	void playerStopWatching(PlayerEntity player);
}
