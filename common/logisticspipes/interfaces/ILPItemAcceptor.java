package logisticspipes.interfaces;

import javax.annotation.Nonnull;

import net.minecraft.item.ItemStack;
import net.minecraft.util.Direction;

import logisticspipes.pipes.basic.LogisticsTileGenericPipe;

public interface ILPItemAcceptor {

	boolean accept(LogisticsTileGenericPipe pipe, Direction from, @Nonnull ItemStack stack);
}
