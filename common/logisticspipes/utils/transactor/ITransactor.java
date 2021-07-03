package logisticspipes.utils.transactor;

import javax.annotation.Nonnull;

import net.minecraft.item.ItemStack;
import net.minecraft.util.Direction;

public interface ITransactor {

	@Nonnull
	ItemStack add(@Nonnull ItemStack stack, Direction orientation, boolean doAdd);
}
