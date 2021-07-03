package logisticspipes.network.abstractpackets;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import logisticspipes.utils.gui.LogisticsBaseGuiScreen;
import logisticspipes.utils.gui.SubGuiScreen;

public abstract class GuiPacket extends ModernPacket {

	public GuiPacket(int id) {
		super(id);
	}

	@OnlyIn(Dist.CLIENT)
	protected <T> T getGui(Class<T> guiClass) {
		GuiScreen currentScreen = Minecraft.getInstance().currentScreen;
		if (currentScreen == null) {
			return null;
		}
		if (guiClass.isAssignableFrom(currentScreen.getClass())) {
			return (T) currentScreen;
		}
		SubGuiScreen subScreen = null;
		if (currentScreen instanceof LogisticsBaseGuiScreen) {
			subScreen = ((LogisticsBaseGuiScreen) currentScreen).getSubGui();
		}
		while (subScreen != null) {
			if (guiClass.isAssignableFrom(subScreen.getClass())) {
				return (T) subScreen;
			}
			subScreen = subScreen.getSubGui();
		}
		return null;
	}
}
