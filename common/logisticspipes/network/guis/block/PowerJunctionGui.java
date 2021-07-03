package logisticspipes.network.guis.block;

import net.minecraft.entity.player.PlayerEntity;

import logisticspipes.blocks.LogisticsPowerJunctionTileEntity;
import logisticspipes.gui.GuiPowerJunction;
import logisticspipes.network.abstractguis.CoordinatesGuiProvider;
import logisticspipes.network.abstractguis.GuiProvider;
import logisticspipes.utils.StaticResolve;
import logisticspipes.utils.gui.DummyContainer;

@StaticResolve
public class PowerJunctionGui extends CoordinatesGuiProvider {

	public PowerJunctionGui(int id) {
		super(id);
	}

	@Override
	public Object getClientGui(PlayerEntity player) {
		return new GuiPowerJunction(player, getTileAs(player.world, LogisticsPowerJunctionTileEntity.class));
	}

	@Override
	public DummyContainer getContainer(PlayerEntity player) {
		DummyContainer dummy = new DummyContainer(player, null, getTileAs(player.world, LogisticsPowerJunctionTileEntity.class));
		dummy.addNormalSlotsForPlayerInventory(8, 80);
		return dummy;
	}

	@Override
	public GuiProvider template() {
		return new PowerJunctionGui(getId());
	}
}
