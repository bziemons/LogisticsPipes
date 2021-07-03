package logisticspipes.blocks;

import javax.annotation.Nonnull;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.world.World;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

import logisticspipes.interfaces.IRotationProvider;
import logisticspipes.network.PacketHandler;
import logisticspipes.network.packets.block.RequestRotationPacket;
import logisticspipes.proxy.MainProxy;
import logisticspipes.proxy.SimpleServiceLocator;
import network.rs485.logisticspipes.world.DoubleCoordinates;

public class LogisticsSolidTileEntity extends TileEntity implements IRotationProvider {

	private boolean addedToNetwork = false;
	private boolean init = false;
	public int rotation = 0;

	public LogisticsSolidTileEntity(TileEntityType<LogisticsSolidTileEntity> tileEntityType) {
		super(tileEntityType);
	}

	@Override
	public void read(@Nonnull CompoundNBT tag) {
		super.read(tag);
		rotation = tag.getInt("rotation");
	}

	@Nonnull
	@Override
	public CompoundNBT write(@Nonnull CompoundNBT tag) {
		tag = super.write(tag);
		tag.putInt("rotation", rotation);
		return tag;
	}

	@Override
	public void updateContainingBlockInfo() {
		super.updateContainingBlockInfo();

		if (!addedToNetwork) {
			addedToNetwork = true;
			SimpleServiceLocator.openComputersProxy.addToNetwork(this);
		}
		DistExecutor.runWhenOn(Dist.CLIENT, () -> () -> {
			if (!init) {
				MainProxy.sendPacketToServer(PacketHandler.getPacket(RequestRotationPacket.class).setBlockPos(pos));
				init = true;
			}
		});
	}

	public DoubleCoordinates getLPPosition() {
		return new DoubleCoordinates(this);
	}

	public World getWorldForHUD() {
		return getWorld();
	}

	@Override
	public int getRotation() {
		return rotation;
	}

	@Override
	public void setRotation(int rotation) {
		this.rotation = rotation;
	}

}
