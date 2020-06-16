package logisticspipes.pipes.upgrades;

import logisticspipes.modules.ModuleActiveSupplier;
import logisticspipes.pipes.PipeItemsSupplierLogistics;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import network.rs485.logisticspipes.api.LogisticsModule;

public class PatternUpgrade implements IPipeUpgrade {

	public static String getName() {
		return "pattern";
	}

	@Override
	public boolean needsUpdate() {
		return false;
	}

	@Override
	public boolean isAllowedForPipe(CoreRoutedPipe pipe) {
		return pipe instanceof PipeItemsSupplierLogistics;
	}

	@Override
	public boolean isAllowedForModule(LogisticsModule pipe) {
		return pipe instanceof ModuleActiveSupplier;
	}

	@Override
	public String[] getAllowedPipes() {
		return new String[] { "supplier" };
	}

	@Override
	public String[] getAllowedModules() {
		return new String[] {};
	}
}
