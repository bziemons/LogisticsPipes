package logisticspipes.interfaces;

import java.util.Objects;

import net.minecraftforge.common.extensions.IForgeItem;

public interface ILogisticsItem extends IForgeItem {

	default String getModelPath() {
		return Objects.requireNonNull(getItem().getRegistryName(), "Registry not set").getPath();
	}

	default int getModelCount() {
		return 1;
	}

}
