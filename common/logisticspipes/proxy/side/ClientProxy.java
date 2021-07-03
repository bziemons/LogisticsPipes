package logisticspipes.proxy.side;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.INetHandler;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.play.ServerPlayNetHandler;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.World;
import net.minecraft.world.dimension.Dimension;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.LogicalSidedProvider;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.registries.ForgeRegistries;

import logisticspipes.LPConstants;
import logisticspipes.LogisticsPipes;
import logisticspipes.gui.GuiCraftingPipe;
import logisticspipes.gui.modules.ModuleBaseGui;
import logisticspipes.gui.popup.SelectItemOutOfList;
import logisticspipes.interfaces.ILogisticsItem;
import logisticspipes.items.ItemLogisticsPipe;
import logisticspipes.modules.LogisticsModule;
import logisticspipes.network.PacketHandler;
import logisticspipes.network.packets.gui.DummyContainerSlotClick;
import logisticspipes.pipefxhandlers.Particles;
import logisticspipes.pipefxhandlers.PipeFXRenderHandler;
import logisticspipes.pipefxhandlers.providers.EntityBlueSparkleFXProvider;
import logisticspipes.pipefxhandlers.providers.EntityGoldSparkleFXProvider;
import logisticspipes.pipefxhandlers.providers.EntityGreenSparkleFXProvider;
import logisticspipes.pipefxhandlers.providers.EntityLightGreenSparkleFXProvider;
import logisticspipes.pipefxhandlers.providers.EntityLightRedSparkleFXProvider;
import logisticspipes.pipefxhandlers.providers.EntityOrangeSparkleFXProvider;
import logisticspipes.pipefxhandlers.providers.EntityRedSparkleFXProvider;
import logisticspipes.pipefxhandlers.providers.EntityVioletSparkleFXProvider;
import logisticspipes.pipefxhandlers.providers.EntityWhiteSparkleFXProvider;
import logisticspipes.pipes.basic.CoreUnroutedPipe;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.proxy.MainProxy;
import logisticspipes.proxy.SimpleServiceLocator;
import logisticspipes.proxy.interfaces.IProxy;
import logisticspipes.renderer.FluidContainerRenderer;
import logisticspipes.renderer.LogisticsRenderPipe;
import logisticspipes.renderer.newpipe.GLRenderListHandler;
import logisticspipes.renderer.newpipe.LogisticsBlockModel;
import logisticspipes.renderer.newpipe.LogisticsNewPipeModel;
import logisticspipes.renderer.newpipe.LogisticsNewRenderPipe;
import logisticspipes.utils.FluidIdentifier;
import logisticspipes.utils.gui.LogisticsBaseGuiScreen;
import logisticspipes.utils.gui.SubGuiScreen;
import logisticspipes.utils.item.ItemIdentifier;
import logisticspipes.utils.item.ItemIdentifierStack;

@OnlyIn(Dist.CLIENT)
public class ClientProxy implements IProxy {

	int renderIndex = 0;

	@Override
	public String getSide() {
		return "Client";
	}

	@Override
	public World getWorld() {
		return Minecraft.getInstance().world;
	}

	@Override
	public void registerTileEntities() {
		LogisticsRenderPipe lrp = new LogisticsRenderPipe();
		ClientRegistry.bindTileEntitySpecialRenderer(LogisticsTileGenericPipe.class, lrp);

		SimpleServiceLocator.setRenderListHandler(new GLRenderListHandler());
	}

	@Override
	public PlayerEntity getClientPlayer() {
		return Minecraft.getInstance().player;
	}

	@Override
	public void registerParticles() {
		PipeFXRenderHandler.registerParticleHandler(Particles.WhiteParticle, new EntityWhiteSparkleFXProvider());
		PipeFXRenderHandler.registerParticleHandler(Particles.RedParticle, new EntityRedSparkleFXProvider());
		PipeFXRenderHandler.registerParticleHandler(Particles.BlueParticle, new EntityBlueSparkleFXProvider());
		PipeFXRenderHandler.registerParticleHandler(Particles.GreenParticle, new EntityGreenSparkleFXProvider());
		PipeFXRenderHandler.registerParticleHandler(Particles.GoldParticle, new EntityGoldSparkleFXProvider());
		PipeFXRenderHandler.registerParticleHandler(Particles.VioletParticle, new EntityVioletSparkleFXProvider());
		PipeFXRenderHandler.registerParticleHandler(Particles.OrangeParticle, new EntityOrangeSparkleFXProvider());
		PipeFXRenderHandler.registerParticleHandler(Particles.LightGreenParticle, new EntityLightGreenSparkleFXProvider());
		PipeFXRenderHandler.registerParticleHandler(Particles.LightRedParticle, new EntityLightRedSparkleFXProvider());
	}

	@Override
	public String getName(ItemIdentifier item) {
		return item.getFriendlyName().getFormattedText();
	}

	@Override
	public void updateNames(ItemIdentifier item, String name) {
		//Not Client Side
	}

	@Override
	public void tick() {
		//Not Client Side
	}

	@Override
	public void sendNameUpdateRequest(PlayerEntity player) {
		//Not Client Side
	}

	@Nullable
	@Override
	public LogisticsTileGenericPipe getPipeInDimensionAt(Dimension dim, BlockPos pos, PlayerEntity player) {
		if (getWorld().dimension == dim) {
			final TileEntity tileEntity = getWorld().getTileEntity(pos);
			if (tileEntity instanceof LogisticsTileGenericPipe) return (LogisticsTileGenericPipe) tileEntity;
		}
		return null;
	}

	@Override
	public void sendBroadCast(String message) {
		if (Minecraft.getInstance().player != null) {
			Minecraft.getInstance().player.sendMessage(new StringTextComponent("[LP] Client: " + message));
		}
	}

	@Override
	public void tickServer() {}

	@Override
	public void tickClient() {
		MainProxy.addTick();
		SimpleServiceLocator.renderListHandler.tick();
	}

	@Override
	public PlayerEntity getPlayerEntityFromNetHandler(INetHandler handler) {
		if (handler instanceof ServerPlayNetHandler) {
			ServerPlayerEntity player = ((ServerPlayNetHandler) handler).player;
			if (player != null) {
				return player;
			}
		}
		return Minecraft.getInstance().player;
	}

	@Override
	public void setIconProviderFromPipe(ItemLogisticsPipe item, CoreUnroutedPipe dummyPipe) {
		item.setPipesIcons(dummyPipe.getIconProvider());
	}

	@Override
	public LogisticsModule getModuleFromGui() {
		if (FMLClientHandler.instance().getClient().currentScreen instanceof ModuleBaseGui) {
			return ((ModuleBaseGui) FMLClientHandler.instance().getClient().currentScreen).getModule();
		}
		if (FMLClientHandler.instance().getClient().currentScreen instanceof GuiCraftingPipe) {
			return ((GuiCraftingPipe) FMLClientHandler.instance().getClient().currentScreen).getCraftingModule();
		}
		return null;
	}

	@Override
	public boolean checkSinglePlayerOwner(String commandSenderName) {
		final MinecraftServer server = LogicalSidedProvider.INSTANCE.get(LogicalSide.SERVER);
		return server.isSinglePlayer() && server instanceof IntegratedServer && !server.getPublic();
	}

	@Override
	public void openFluidSelectGui(final int slotId) {
		if (Minecraft.getInstance().currentScreen instanceof LogisticsBaseGuiScreen) {
			final List<ItemIdentifierStack> list = new ArrayList<>();
			for (FluidIdentifier fluid : FluidIdentifier.all()) {
				if (fluid == null) {
					continue;
				}
				list.add(fluid.getItemIdentifier().makeStack(1));
			}
			SelectItemOutOfList subGui = new SelectItemOutOfList(list, slot -> {
				if (slot == -1) return;
				MainProxy.sendPacketToServer(PacketHandler.getPacket(DummyContainerSlotClick.class).setSlotId(slotId).setStack(list.get(slot).makeNormalStack()).setButton(0));
			});
			LogisticsBaseGuiScreen gui = (LogisticsBaseGuiScreen) Minecraft.getInstance().currentScreen;
			if (!gui.hasSubGui()) {
				gui.setSubGui(subGui);
			} else {
				SubGuiScreen nextGui = gui.getSubGui();
				while (nextGui.hasSubGui()) {
					nextGui = nextGui.getSubGui();
				}
				nextGui.setSubGui(subGui);
			}
		} else {
			throw new UnsupportedOperationException(String.valueOf(Minecraft.getInstance().currentScreen));
		}
	}

	@Override
	public void registerModels() {
		ForgeRegistries.ITEMS.getValuesCollection().stream()
				.filter(item -> item.getRegistryName().getNamespace().equals(LPConstants.LP_MOD_ID))
				.filter(item -> item instanceof ILogisticsItem)
				.forEach(item -> registerModels((ILogisticsItem) item));
	}

	private void registerModels(ILogisticsItem item) {
		int mc = item.getModelCount();
		for (int i = 0; i < mc; i++) {
			String modelPath = item.getModelPath();
			if (mc > 1) {
				String resourcePath = item.getItem().getRegistryName().getPath();
				if (modelPath.matches(String.format(".*%s/%s", resourcePath, resourcePath))) {
					modelPath = String.format("%s/%d", modelPath.substring(0, modelPath.length() - resourcePath.length() - 1), i);
				} else {
					modelPath = String.format("%s.%d", modelPath, i);
				}
			}
			ModelLoader.setCustomModelResourceLocation(item.getItem(), i, new ModelResourceLocation(new ResourceLocation(item.getItem().getRegistryName().getNamespace(), modelPath), "inventory"));
		}
	}

	@Override
	public void registerTextures() {
		LogisticsPipes.textures.registerBlockIcons(Minecraft.getInstance().getTextureMap());
		LogisticsNewRenderPipe.registerTextures(Minecraft.getInstance().getTextureMap());
		LogisticsNewPipeModel.registerTextures(Minecraft.getInstance().getTextureMap());
		SimpleServiceLocator.thermalDynamicsProxy.registerTextures(Minecraft.getInstance().getTextureMap());
		renderIndex++;
	}

	@Override
	public void initModelLoader() {
		ModelLoaderRegistry.registerLoader(new LogisticsNewPipeModel.LogisticsNewPipeModelLoader());
		ModelLoaderRegistry.registerLoader(new LogisticsBlockModel.Loader());
		ModelLoaderRegistry.registerLoader(new FluidContainerRenderer.FluidContainerRendererModelLoader());
	}

	@Override
	public int getRenderIndex() {
		return renderIndex;
	}

}
