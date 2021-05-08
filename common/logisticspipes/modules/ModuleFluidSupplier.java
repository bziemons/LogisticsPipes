package logisticspipes.modules;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nonnull;

import net.minecraft.inventory.IInventory;

import logisticspipes.interfaces.IClientInformationProvider;
import logisticspipes.network.NewGuiHandler;
import logisticspipes.network.abstractguis.ModuleCoordinatesGuiProvider;
import logisticspipes.network.abstractguis.ModuleInHandGuiProvider;
import logisticspipes.network.guis.module.inpipe.FluidSupplierSlot;
import logisticspipes.pipes.PipeLogisticsChassis.ChassiTargetInformation;
import logisticspipes.utils.SinkReply;
import logisticspipes.utils.SinkReply.FixedPriority;
import logisticspipes.utils.item.ItemIdentifierInventory;
import network.rs485.logisticspipes.module.Gui;
import network.rs485.logisticspipes.property.InventoryProperty;
import network.rs485.logisticspipes.property.Property;

public class ModuleFluidSupplier extends LogisticsModule implements IClientInformationProvider, Gui {

	private final InventoryProperty filterInventory = new InventoryProperty(
			new ItemIdentifierInventory(9, "Requested liquids", 1), "");

	private SinkReply _sinkReply;

	@Nonnull
	@Override
	public String getLPName() {
		throw new RuntimeException("Cannot get LP name for " + this);
	}

	@Nonnull
	@Override
	public List<Property<?>> getProperties() {
		return Collections.singletonList(filterInventory);
	}

	@Nonnull
	public IInventory getFilterInventory() {
		return filterInventory;
	}

	@Override
	public void registerPosition(@Nonnull ModulePositionType slot, int positionInt) {
		super.registerPosition(slot, positionInt);
		_sinkReply = new SinkReply(FixedPriority.ItemSink, 0, 0, 0, new ChassiTargetInformation(getPositionInt()));
	}

	@Override
	public void tick() {}

	@Override
	public @Nonnull
	List<String> getClientInformation() {
		List<String> list = new ArrayList<>();
		list.add("Supplied: ");
		list.add("<inventory>");
		list.add("<that>");
		return list;
	}

	@Override
	public boolean recievePassive() {
		return true;
	}

	@Nonnull
	@Override
	public ModuleCoordinatesGuiProvider getPipeGuiProvider() {
		return NewGuiHandler.getGui(FluidSupplierSlot.class);
	}

	@Nonnull
	@Override
	public ModuleInHandGuiProvider getInHandGuiProvider() {
		throw new UnsupportedOperationException("Fluid Supplier GUI cannot be opened in hand");
	}

}
