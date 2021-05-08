package logisticspipes.modules;

import java.util.Collections;
import javax.annotation.Nonnull;

import org.jetbrains.annotations.NotNull;

import logisticspipes.pipes.PipeLogisticsChassis.ChassiTargetInformation;
import logisticspipes.utils.SinkReply;
import logisticspipes.utils.SinkReply.FixedPriority;
import network.rs485.logisticspipes.property.Property;

public class ModulePolymorphicItemSink extends LogisticsModule {

	private SinkReply _sinkReply;

	public static String getName() {
		return "item_sink_polymorphic";
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
		_sinkReply = new SinkReply(FixedPriority.ItemSink, 0, 3, 0, new ChassiTargetInformation(getPositionInt()));
	}

	@Override
	public void tick() {}

	@Override
	public final int getX() {
		return _service.getX();
	}

	@Override
	public final int getY() {
		return _service.getY();
	}

	@Override
	public final int getZ() {
		return _service.getZ();
	}

	//TODO: SINK UNDAMAGED MATCH CORRECTLY!

	@Override
	public boolean recievePassive() {
		return true;
	}

}
