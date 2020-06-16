package logisticspipes.gui.modules;

import java.io.IOException;

import net.minecraft.inventory.Container;

import lombok.Getter;

import logisticspipes.utils.gui.LogisticsBaseGuiScreen;
import network.rs485.logisticspipes.api.LogisticsModule;

public abstract class ModuleBaseGui extends LogisticsBaseGuiScreen {

	@Getter
	protected LogisticsModule module;

	public ModuleBaseGui(Container par1Container, LogisticsModule module) {
		super(par1Container);
		this.module = module;
	}

	@Override
	protected void keyTyped(char c, int i) throws IOException {
		if (module == null) {
			super.keyTyped(c, i);
			return;
		}
		if (i == 1 || c == 'e') {
			super.keyTyped(c, i);
//			if (module.getSlot() == AbstractModule.ModulePositionType.SLOT) {
//				MainProxy.sendPacketToServer(PacketHandler.getPacket(GuiOpenChassie.class).setBlockPos(module.getBlockPos()));
//			}
		}
	}
}
