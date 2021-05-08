package logisticspipes.modules;

import java.util.Collections;
import java.util.List;
import javax.annotation.Nonnull;

import org.jetbrains.annotations.NotNull;

import logisticspipes.pipes.PipeLogisticsChassis.ChassiTargetInformation;
import logisticspipes.utils.SinkReply;
import logisticspipes.utils.SinkReply.FixedPriority;
import network.rs485.logisticspipes.property.Property;

public class ModuleEnchantmentSink extends LogisticsModule {

	private SinkReply _sinkReply;

	public static String getName() {
		return "enchantment_sink";
	}

	@Nonnull
	@Override
	public String getLPName() {
		return getName();
	}

	@NotNull
	@Override
	public List<Property<?>> getProperties() {
		return Collections.emptyList();
	}

	@Override
	public void registerPosition(@Nonnull ModulePositionType slot, int positionInt) {
		super.registerPosition(slot, positionInt);
		_sinkReply = new SinkReply(FixedPriority.EnchantmentItemSink, 0, 1, 0, new ChassiTargetInformation(getPositionInt()));
	}

	@Override
	public void tick() {}

	@Override
	public boolean recievePassive() {
		return true;
	}

	@Override
	public boolean hasEffect() {
		return true;
	}

}
