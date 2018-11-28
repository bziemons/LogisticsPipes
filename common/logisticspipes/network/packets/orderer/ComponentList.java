package logisticspipes.network.packets.orderer;

import java.util.ArrayList;
import java.util.Collection;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.text.TextComponentString;

import net.minecraftforge.fml.client.FMLClientHandler;

import lombok.Getter;
import lombok.Setter;
import static network.rs485.logisticspipes.resource.ResourceUtil.ColorCode.MISSING;
import static network.rs485.logisticspipes.resource.ResourceUtil.ColorCode.SUCCESS;

import logisticspipes.asm.ClientSideOnlyMethodContent;
import logisticspipes.config.Configs;
import logisticspipes.gui.orderer.GuiOrderer;
import logisticspipes.gui.orderer.GuiRequestTable;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.request.resources.IResource;
import logisticspipes.request.resources.ResourceNetwork;
import logisticspipes.utils.StaticResolve;
import network.rs485.logisticspipes.resource.ResourceUtil;
import network.rs485.logisticspipes.util.LPDataInput;
import network.rs485.logisticspipes.util.LPDataOutput;

@StaticResolve
public class ComponentList extends ModernPacket {

	@Getter
	@Setter
	private Collection<IResource> used = new ArrayList<>();

	@Getter
	@Setter
	private Collection<IResource> missing = new ArrayList<>();

	public ComponentList(int id) {
		super(id);
	}

	@Override
	public ModernPacket template() {
		return new ComponentList(getId());
	}

	@Override
	@ClientSideOnlyMethodContent
	public void processPacket(EntityPlayer player) {
		if (Configs.DISPLAY_POPUP && FMLClientHandler.instance().getClient().currentScreen instanceof GuiOrderer) {
			((GuiOrderer) FMLClientHandler.instance().getClient().currentScreen)
					.handleSimulateAnswer(used, missing, (GuiOrderer) FMLClientHandler.instance().getClient().currentScreen, player);
		} else if (Configs.DISPLAY_POPUP && FMLClientHandler.instance().getClient().currentScreen instanceof GuiRequestTable) {
			((GuiRequestTable) FMLClientHandler.instance().getClient().currentScreen)
					.handleSimulateAnswer(used, missing, (GuiRequestTable) FMLClientHandler.instance().getClient().currentScreen, player);
		} else {
			for (IResource item : used) {
				player.sendMessage(new TextComponentString("Component: " + ResourceUtil.getDisplayText(SUCCESS, item)));
			}
			for (IResource item : missing) {
				player.sendMessage(new TextComponentString("Missing: " + ResourceUtil.getDisplayText(MISSING, item)));
			}
		}
	}

	@Override
	public void writeData(LPDataOutput output) {
		output.writeCollection(used);
		output.writeCollection(missing);
	}

	@Override
	public void readData(LPDataInput input) {
		used = input.readArrayList(ResourceNetwork::readResource);
		missing = input.readArrayList(ResourceNetwork::readResource);
	}
}
