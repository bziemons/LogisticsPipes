package logisticspipes.logisticspipes;

import java.util.List;
import java.util.Objects;
import java.util.Random;
import javax.annotation.Nonnull;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.nbt.StringNBT;

import net.minecraftforge.fml.common.thread.EffectiveSide;

import logisticspipes.interfaces.IClientInformationProvider;
import logisticspipes.modules.LogisticsModule;

public class ItemModuleInformationManager {

	public static void saveInformation(@Nonnull ItemStack stack, LogisticsModule module) {
		if (module == null) {
			return;
		}
		CompoundNBT nbt = new CompoundNBT();
		module.writeToNBT(nbt);
		if (nbt.equals(new CompoundNBT())) {
			return;
		}
		if (EffectiveSide.get().isClient()) {
			ListNBT list = new ListNBT();
			String info1 = "Please reopen the window";
			String info2 = "to see the information.";
			list.add(new StringNBT(info1));
			list.add(new StringNBT(info2));
			if (!stack.hasTag()) {
				stack.setTag(new CompoundNBT());
			}
			CompoundNBT tag = Objects.requireNonNull(stack.getTag());
			tag.setTag("informationList", list);
			tag.putDouble("Random-Stack-Prevent", new Random().nextDouble());
			return;
		}
		if (!stack.hasTag()) {
			stack.setTag(new CompoundNBT());
		}
		CompoundNBT tag = Objects.requireNonNull(stack.getTag());
		tag.setTag("moduleInformation", nbt);
		if (module instanceof IClientInformationProvider) {
			List<String> information = ((IClientInformationProvider) module).getClientInformation();
			if (information.size() > 0) {
				ListNBT list = new ListNBT();
				for (String info : information) {
					list.add(new StringNBT(info));
				}
				tag.setTag("informationList", list);
			}
		}
		tag.putDouble("Random-Stack-Prevent", new Random().nextDouble());
	}

	public static void readInformation(@Nonnull ItemStack stack, LogisticsModule module) {
		if (module == null) {
			return;
		}
		if (stack.hasTag()) {
			CompoundNBT nbt = Objects.requireNonNull(stack.getTag());
			if (nbt.contains("moduleInformation")) {
				CompoundNBT moduleInformation = nbt.getCompound("moduleInformation");
				module.readFromNBT(moduleInformation);
			}
		}
	}
}
