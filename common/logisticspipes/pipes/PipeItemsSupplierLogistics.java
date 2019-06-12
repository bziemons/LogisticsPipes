/**
 * Copyright (c) Krapht, 2011
 * 
 * "LogisticsPipes" is distributed under the terms of the Minecraft Mod Public
 * License 1.0, or MMPL. Please check the contents of the license located in
 * http://www.mod-buildcraft.com/MMPL-1.0.txt
 */

package logisticspipes.pipes;

import java.util.List;

import net.minecraft.item.Item;
import net.minecraft.nbt.NBTTagCompound;

import logisticspipes.modules.ModuleActiveSupplier;
import logisticspipes.modules.abstractmodules.LogisticsModule;
import logisticspipes.modules.abstractmodules.LogisticsModule.ModulePositionType;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.pipes.basic.debug.StatusEntry;
import logisticspipes.textures.Textures;
import logisticspipes.textures.Textures.TextureType;

public class PipeItemsSupplierLogistics extends CoreRoutedPipe {

	private ModuleActiveSupplier module;

	public PipeItemsSupplierLogistics(Item item) {
		super(item);
		module = new ModuleActiveSupplier();
		module.registerHandler(this, this);
		module.registerPosition(ModulePositionType.IN_PIPE, 0);
	}

	@Override
	public TextureType getCenterTexture() {
		return Textures.LOGISTICSPIPE_SUPPLIER_TEXTURE;
	}

	@Override
	public LogisticsModule getLogisticsModule() {
		return module;
	}

	@Override
	public ItemSendMode getItemSendMode() {
		return ItemSendMode.Normal;
	}

	@Override
	public void readFromNBT(NBTTagCompound nbttagcompound) {
		super.readFromNBT(nbttagcompound);
		module.readFromNBT(nbttagcompound);
	}

	@Override
	public void writeToNBT(NBTTagCompound nbttagcompound) {
		super.writeToNBT(nbttagcompound);
		module.writeToNBT(nbttagcompound);
	}

	@Override
	public void addStatusInformation(List<StatusEntry> status) {
		super.addStatusInformation(status);
		module.addStatusInformation(status);
	}
}
