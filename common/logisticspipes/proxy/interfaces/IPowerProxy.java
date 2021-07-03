package logisticspipes.proxy.interfaces;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;

import logisticspipes.proxy.cofh.subproxies.ICoFHEnergyReceiver;
import logisticspipes.proxy.cofh.subproxies.ICoFHEnergyStorage;

public interface IPowerProxy {

	boolean isEnergyReceiver(TileEntity tile, Direction face);

	ICoFHEnergyReceiver getEnergyReceiver(TileEntity tile, Direction face);

	ICoFHEnergyStorage getEnergyStorage(int i);

	boolean isAvailable();
}
