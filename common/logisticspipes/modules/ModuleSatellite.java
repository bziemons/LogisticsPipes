package logisticspipes.modules;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nonnull;

import net.minecraft.item.ItemStack;

import org.jetbrains.annotations.NotNull;

import logisticspipes.interfaces.IPipeServiceProvider;
import logisticspipes.utils.SinkReply;
import logisticspipes.utils.SinkReply.FixedPriority;
import logisticspipes.utils.item.ItemIdentifier;
import network.rs485.logisticspipes.connection.LPNeighborTileEntityKt;
import network.rs485.logisticspipes.property.Property;

public class ModuleSatellite extends LogisticsModule {

	private final SinkReply _sinkReply = new SinkReply(FixedPriority.ItemSink, 0, true, false, 1, 0, null);

	@Nonnull
	@Override
	public String getLPName() {
		throw new RuntimeException("Cannot get LP name for " + this);
	}

	@NotNull
	@Override
	public List<Property<?>> getProperties() {
		return Collections.emptyList();
	}

	private SinkReply _sinkReply = new SinkReply(FixedPriority.ItemSink, 0, 1, 0, null);

	private int spaceFor(@Nonnull ItemStack stack, ItemIdentifier item, boolean includeInTransit) {
		final IPipeServiceProvider service = Objects.requireNonNull(_service);
		int count = service.getAvailableAdjacent().inventories().stream()
				.map(neighbor -> LPNeighborTileEntityKt.sneakyInsertion(neighbor).from(getUpgradeManager()))
				.map(LPNeighborTileEntityKt::getInventoryUtil)
				.filter(Objects::nonNull)
				.map(util -> util.roomForItem(stack))
				.reduce(Integer::sum).orElse(0);
		if (includeInTransit) {
			count -= service.countOnRoute(item);
		}
		return count;
	}

	@Override
	public void tick() {}

	@Override
	public boolean recievePassive() {
		return false;
	}

}
