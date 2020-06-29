package logisticspipes.pipes.upgrades;

import logisticspipes.pipes.PipeLogisticsChassi;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import network.rs485.logisticspipes.api.LogisticsModule;

public class UpgradeModuleUpgrade implements IPipeUpgrade {

	public static String getName() {
		return "module_upgrade";
	}

	@Override
	public boolean needsUpdate() {
		return false;
	}

	@Override
	public boolean isAllowedForPipe(CoreRoutedPipe pipe) {
		return pipe instanceof PipeLogisticsChassi;
	}

	@Override
	public boolean isAllowedForModule(LogisticsModule pipe) {
		return false;
	}

	@Override
	public String[] getAllowedPipes() {
		return new String[] { "chassis" };
	}

	@Override
	public String[] getAllowedModules() {
		return new String[] {};
	}
}
