package logisticspipes.pipes.signs;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.Direction;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.renderer.LogisticsRenderPipe;

public interface IPipeSign {

	// Methods used when assigning a sign
	boolean isAllowedFor(CoreRoutedPipe pipe);

	void addSignTo(CoreRoutedPipe pipe, Direction dir, PlayerEntity player);

	// For Final Pipe
	void readFromNBT(CompoundNBT tag);

	void writeToNBT(CompoundNBT tag);

	void init(CoreRoutedPipe pipe, Direction dir);

	void activate(PlayerEntity player);

	ModernPacket getPacket();

	void updateServerSide();

	@OnlyIn(Dist.CLIENT)
	void render(CoreRoutedPipe pipe, LogisticsRenderPipe renderer);

}
