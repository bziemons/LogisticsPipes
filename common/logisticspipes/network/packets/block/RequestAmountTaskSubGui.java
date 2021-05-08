package logisticspipes.network.packets.block;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeSet;

import net.minecraft.entity.player.EntityPlayer;

import logisticspipes.blocks.stats.LogisticsStatisticsTileEntity;
import logisticspipes.network.PacketHandler;
import logisticspipes.network.abstractpackets.CoordinatesPacket;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.proxy.MainProxy;
import logisticspipes.utils.StaticResolve;
import logisticspipes.utils.item.ItemIdentifier;
import logisticspipes.utils.item.ItemIdentifierStack;

@StaticResolve
public class RequestAmountTaskSubGui extends CoordinatesPacket {

	public RequestAmountTaskSubGui(int id) {
		super(id);
	}

	@Override
	public void processPacket(EntityPlayer player) {
		LogisticsStatisticsTileEntity tile = this.getTileAs(player.getEntityWorld(), LogisticsStatisticsTileEntity.class);
		CoreRoutedPipe pipe = tile.getConnectedPipe();
		if (pipe == null) {
			return;
		}

		// TODO PROVIDE REFACTOR
		Map<ItemIdentifier, Integer> _availableItems = Collections.emptyMap(); // SimpleServiceLocator.logisticsManager.getAvailableItems(pipe.getRouter().getIRoutersByCost());
		List<ItemIdentifier> _craftableItems = Collections.emptyList(); // SimpleServiceLocator.logisticsManager.getCraftableItems(pipe.getRouter().getIRoutersByCost());

		TreeSet<ItemIdentifierStack> _allItems = new TreeSet<>();

		for (Entry<ItemIdentifier, Integer> item : _availableItems.entrySet()) {
			ItemIdentifierStack newStack = item.getKey().makeStack(item.getValue());
			_allItems.add(newStack);
		}

		for (ItemIdentifier item : _craftableItems) {
			if (_availableItems.containsKey(item)) {
				continue;
			}
			_allItems.add(item.makeStack(1));
		}

		MainProxy.sendPacketToPlayer(PacketHandler.getPacket(AmountTaskSubGui.class).setIdentSet(_allItems), player);
	}

	@Override
	public ModernPacket template() {
		return new RequestAmountTaskSubGui(getId());
	}
}
