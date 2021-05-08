/*
 * Copyright (c) Krapht, 2011
 * "LogisticsPipes" is distributed under the terms of the Minecraft Mod Public
 * License 1.0, or MMPL. Please check the contents of the license located in
 * http://www.mod-buildcraft.com/MMPL-1.0.txt
 */

package logisticspipes.pipes;

import java.util.List;

import net.minecraft.item.Item;
import net.minecraft.nbt.NBTTagCompound;

import logisticspipes.modules.LogisticsModule.ModulePositionType;
import logisticspipes.modules.ModuleActiveSupplier;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.pipes.basic.debug.StatusEntry;
import logisticspipes.textures.Textures;
import logisticspipes.textures.Textures.TextureType;

public class PipeItemsSupplierLogistics extends CoreRoutedPipe {

	private ModuleActiveSupplier supplierModule;

	public PipeItemsSupplierLogistics(Item item) {
		super(item);
		supplierModule = new ModuleActiveSupplier();
		supplierModule.registerHandler(this, this);
		supplierModule.registerPosition(ModulePositionType.IN_PIPE, 0);
	}

	@Override
	public TextureType getCenterTexture() {
		return Textures.LOGISTICSPIPE_SUPPLIER_TEXTURE;
	}

	@Override
	public ModuleActiveSupplier getLogisticsModule() {
		return supplierModule;
	}

	@Override
	public ItemSendMode getItemSendMode() {
		return ItemSendMode.Normal;
	}

	@Override
	public void readFromNBT(NBTTagCompound nbttagcompound) {
		super.readFromNBT(nbttagcompound);
		supplierModule.readFromNBT(nbttagcompound);
	}

	@Override
	public void writeToNBT(NBTTagCompound nbttagcompound) {
		super.writeToNBT(nbttagcompound);
		supplierModule.writeToNBT(nbttagcompound);
	}

	@Override
	public void addStatusInformation(List<StatusEntry> status) {
		super.addStatusInformation(status);
		supplierModule.addStatusInformation(status);
	}
}
