package logisticspipes.pipes.upgrades;

import logisticspipes.pipes.basic.CoreRoutedPipe;
import network.rs485.logisticspipes.api.LogisticsModule;

public interface IPipeUpgrade {

	boolean needsUpdate();

	boolean isAllowedForPipe(CoreRoutedPipe pipe);

	boolean isAllowedForModule(LogisticsModule pipe);

	String[] getAllowedPipes();

	String[] getAllowedModules();
}
