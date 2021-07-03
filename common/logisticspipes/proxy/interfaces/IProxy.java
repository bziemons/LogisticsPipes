package logisticspipes.proxy.interfaces;

import javax.annotation.Nullable;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.INetHandler;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.dimension.Dimension;

import logisticspipes.items.ItemLogisticsPipe;
import logisticspipes.modules.LogisticsModule;
import logisticspipes.pipes.basic.CoreUnroutedPipe;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.utils.item.ItemIdentifier;

public interface IProxy {

	String getSide();

	World getWorld();

	void registerTileEntities();

	PlayerEntity getClientPlayer();

	void registerParticles();

	String getName(ItemIdentifier item);

	void updateNames(ItemIdentifier item, String name);

	void tick();

	void sendNameUpdateRequest(PlayerEntity player);

	@Nullable
	LogisticsTileGenericPipe getPipeInDimensionAt(Dimension dim, BlockPos pos, PlayerEntity player);

	void sendBroadCast(String message);

	void tickServer();

	void tickClient();

	PlayerEntity getPlayerEntityFromNetHandler(INetHandler handler);

	void setIconProviderFromPipe(ItemLogisticsPipe item, CoreUnroutedPipe dummyPipe);

	LogisticsModule getModuleFromGui();

	boolean checkSinglePlayerOwner(String commandSenderName);

	void openFluidSelectGui(int slotId);

	default void registerModels() {}

	void registerTextures();

	void initModelLoader();

	int getRenderIndex();
}
