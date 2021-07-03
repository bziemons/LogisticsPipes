package logisticspipes.network.guis.pipe;

import net.minecraft.entity.player.PlayerEntity;

import logisticspipes.gui.GuiInvSysConnector;
import logisticspipes.network.abstractguis.CoordinatesGuiProvider;
import logisticspipes.network.abstractguis.GuiProvider;
import logisticspipes.pipes.PipeItemsInvSysConnector;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.utils.StaticResolve;
import logisticspipes.utils.gui.DummyContainer;

@StaticResolve
public class InvSysConGuiProvider extends CoordinatesGuiProvider {

	public InvSysConGuiProvider(int id) {
		super(id);
	}

	@Override
	public Object getClientGui(PlayerEntity player) {
		LogisticsTileGenericPipe pipe = getTileAs(player.world, LogisticsTileGenericPipe.class);
		if (!(pipe.pipe instanceof PipeItemsInvSysConnector)) {
			return null;
		}
		return new GuiInvSysConnector(player, (PipeItemsInvSysConnector) pipe.pipe);
	}

	@Override
	public DummyContainer getContainer(PlayerEntity player) {
		LogisticsTileGenericPipe pipe = getTileAs(player.world, LogisticsTileGenericPipe.class);
		if (!(pipe.pipe instanceof PipeItemsInvSysConnector)) {
			return null;
		}
		DummyContainer dummy = new DummyContainer(player, null, (PipeItemsInvSysConnector) pipe.pipe);

		dummy.addNormalSlotsForPlayerInventory(0, 50);

		return dummy;

	}

	@Override
	public GuiProvider template() {
		return new InvSysConGuiProvider(getId());
	}
}
