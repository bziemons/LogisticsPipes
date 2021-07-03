package logisticspipes.pipes.signs;

import java.util.List;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.Direction;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import com.mojang.blaze3d.platform.GlStateManager;

import logisticspipes.modules.LogisticsModule.ModulePositionType;
import logisticspipes.modules.ModuleCrafter;
import logisticspipes.network.PacketHandler;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.network.packets.cpipe.CPipeSatelliteImportBack;
import logisticspipes.pipes.PipeItemsCraftingLogistics;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.renderer.LogisticsRenderPipe;
import logisticspipes.utils.item.ItemIdentifierStack;

public class CraftingPipeSign implements IPipeSign {

	public CoreRoutedPipe pipe;
	public Direction dir;

	@Override
	public boolean isAllowedFor(CoreRoutedPipe pipe) {
		return pipe instanceof PipeItemsCraftingLogistics;
	}

	@Override
	public void addSignTo(CoreRoutedPipe pipe, Direction dir, PlayerEntity player) {
		pipe.addPipeSign(dir, new CraftingPipeSign(), player);
	}

	@Override
	public void readFromNBT(CompoundNBT tag) {}

	@Override
	public void writeToNBT(CompoundNBT tag) {}

	@Override
	public ModernPacket getPacket() {
		PipeItemsCraftingLogistics cpipe = (PipeItemsCraftingLogistics) pipe;
		return PacketHandler.getPacket(CPipeSatelliteImportBack.class)
				.setInventory(cpipe.getDummyInventory())
				.setType(ModulePositionType.IN_PIPE)
				.setPosX(cpipe.getX())
				.setPosY(cpipe.getY())
				.setPosZ(cpipe.getZ());
	}

	@Override
	public void updateServerSide() {}

	@Override
	public void init(CoreRoutedPipe pipe, Direction dir) {
		this.pipe = pipe;
		this.dir = dir;
	}

	@Override
	public void activate(PlayerEntity player) {}

	@Override
	@OnlyIn(Dist.CLIENT)
	public void render(CoreRoutedPipe pipe, LogisticsRenderPipe renderer) {
		PipeItemsCraftingLogistics cpipe = (PipeItemsCraftingLogistics) pipe;
		FontRenderer var17 = renderer.getFontRenderer();
		if (cpipe != null) {
			List<ItemIdentifierStack> craftables = cpipe.getCraftedItems();

			String name = "";
			if (craftables != null && craftables.size() > 0) {
				ItemStack itemstack = craftables.get(0).unsafeMakeNormalStack();

				renderer.renderItemStackOnSign(itemstack);
				Item item = itemstack.getItem();

				GlStateManager.depthMask(false);
				GlStateManager.rotatef(-180.0F, 1.0F, 0.0F, 0.0F);
				GlStateManager.translatef(0.5F, +0.08F, 0.0F);
				GlStateManager.scalef(1.0F / 90.0F, 1.0F / 90.0F, 1.0F / 90.0F);

				try {
					name = item.getItemStackDisplayName(itemstack);
				} catch (Exception e) {
					try {
						name = item.getTranslationKey();
					} catch (Exception ignored) {}
				}

				var17.drawString(String.format("ID: %d", Item.getIdFromItem(item)), -var17.getStringWidth(String.format("ID: %d", Item.getIdFromItem(item))) / 2, 0 * 10 - 4 * 5, 0);
				ModuleCrafter logisticsMod = cpipe.getLogisticsModule();
				/*if (logisticsMod.satelliteId != 0) {
					var17.drawString("Sat ID: " + String.valueOf(logisticsMod.satelliteId), -var17.getStringWidth("Sat ID: " + String.valueOf(logisticsMod.satelliteId)) / 2, 1 * 10 - 4 * 5, 0);
				}
				*/
			} else {
				GlStateManager.rotatef(-180.0F, 1.0F, 0.0F, 0.0F);
				GlStateManager.translatef(0.5F, +0.08F, 0.0F);
				GlStateManager.scalef(1.0F / 90.0F, 1.0F / 90.0F, 1.0F / 90.0F);
				name = "Empty";
			}

			name = renderer.cut(name, var17);

			var17.drawString(name, -var17.getStringWidth(name) / 2 - 15, 3 * 10 - 4 * 5, 0);

			GlStateManager.depthMask(true);
			GlStateManager.color4f(1.0F, 1.0F, 1.0F, 1.0F);
		}
	}

}
