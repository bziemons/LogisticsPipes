package logisticspipes.pipes;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.util.text.StringTextComponent;

import logisticspipes.LogisticsPipes;
import logisticspipes.interfaces.routing.IRequestFluid;
import logisticspipes.network.GuiIDs;
import logisticspipes.pipes.basic.fluid.FluidRoutedPipe;
import logisticspipes.proxy.MainProxy;
import logisticspipes.security.SecuritySettings;
import logisticspipes.textures.Textures;
import logisticspipes.textures.Textures.TextureType;
import logisticspipes.utils.FluidIdentifier;

public class PipeFluidRequestLogistics extends FluidRoutedPipe implements IRequestFluid {

	public PipeFluidRequestLogistics(Item item) {
		super(item);
	}

	public void openGui(PlayerEntity player) {
		PlayerEntity.openGui(LogisticsPipes.instance, GuiIDs.GUI_Fluid_Orderer_ID, getWorld(), getX(), getY(), getZ());
	}

	@Override
	public boolean handleClick(PlayerEntity player, SecuritySettings settings) {
		if (MainProxy.isServer(getWorld())) {
			if (settings == null || settings.openRequest) {
				openGui(PlayerEntity);
			} else {
				PlayerEntity.sendMessage(new StringTextComponent("Permission denied"));
			}
		}
		return true;
	}

	@Override
	public TextureType getCenterTexture() {
		return Textures.LOGISTICSPIPE_LIQUID_REQUEST;
	}

	@Override
	public void sendFailed(FluidIdentifier value1, Integer value2) {
		//Request Pipe doesn't handle this.
	}

	@Override
	public boolean canInsertToTanks() {
		return true;
	}

	@Override
	public boolean canInsertFromSideToTanks() {
		return true;
	}

	@Override
	public boolean canReceiveFluid() {
		return false;
	}
}
