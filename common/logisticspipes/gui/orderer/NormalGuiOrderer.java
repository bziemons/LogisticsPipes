package logisticspipes.gui.orderer;

import java.io.IOException;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.entity.player.EntityPlayer;

import logisticspipes.network.PacketHandler;
import logisticspipes.network.packets.orderer.OrdererRefreshRequestPacket;
import logisticspipes.proxy.MainProxy;
import logisticspipes.utils.gui.SmallGuiButton;
import logisticspipes.utils.item.ItemIdentifier;

public class NormalGuiOrderer extends GuiOrderer {

	private enum DisplayOptions {
		Both("Both"),
		SupplyOnly("Supply"),
		CraftOnly("Craft");

		private String displayString;

		DisplayOptions(String displayString) {
			this.displayString = displayString;
		}

		@Override
		public String toString() {
			return this.displayString;
		}
	}

	protected DisplayOptions currentDisplayOption = DisplayOptions.Both;

	public NormalGuiOrderer(int x, int y, int z, int dim, EntityPlayer entityPlayer) {
		super(x, y, z, dim, entityPlayer);
		refreshItems();
	}

	@Override
	@SuppressWarnings("unchecked")
	public void initGui() {
		super.initGui();
		buttonList.add(new SmallGuiButton(3, guiLeft + 10, bottom - 15, 46, 10, "Refresh")); // Refresh
		buttonList.add(new SmallGuiButton(13, guiLeft + 10, bottom - 28, 46, 10, "Content")); // Component
		buttonList.add(new SmallGuiButton(9, guiLeft + 10, bottom - 41, 46, 10, currentDisplayOption.displayString));
	}

	@Override
	public void refreshItems() {
		MainProxy.sendPacketToServer(PacketHandler.getPacket(OrdererRefreshRequestPacket.class)
				.setInteger2(currentDisplayOption.ordinal()).setInteger(dimension).setPosX(xCoord).setPosY(yCoord).setPosZ(zCoord));
	}

	@Override
	protected void actionPerformed(GuiButton guibutton) throws IOException {
		super.actionPerformed(guibutton);
		if (guibutton.id == 9) {
			DisplayOptions[] displayOptions = DisplayOptions.values();
			currentDisplayOption = displayOptions[currentDisplayOption.ordinal() + 1 % displayOptions.length];
			guibutton.displayString = currentDisplayOption.displayString;
			refreshItems();
		}
	}

	@Override
	public void specialItemRendering(ItemIdentifier item, int x, int y) {}
}
