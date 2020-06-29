package logisticspipes.network.abstractpackets;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;

import lombok.Getter;
import lombok.Setter;

import logisticspipes.LogisticsPipes;
import logisticspipes.modules.AbstractModule.ModulePositionType;
import logisticspipes.network.exception.TargetNotFoundException;
import logisticspipes.pipes.PipeLogisticsChassi;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.proxy.MainProxy;
import logisticspipes.utils.gui.DummyModuleContainer;
import network.rs485.logisticspipes.api.LogisticsModule;
import network.rs485.logisticspipes.util.LPDataInput;
import network.rs485.logisticspipes.util.LPDataOutput;

public abstract class ModuleCoordinatesPacket extends CoordinatesPacket {

	@Getter
	@Setter
	private ModulePositionType type;

	@Getter
	@Setter
	private int positionInt;
	private boolean moduleBased = false;

	public ModuleCoordinatesPacket(int id) {
		super(id);
	}

	@Override
	public void writeData(LPDataOutput output) {
		super.writeData(output);
		output.writeBoolean(type != null);
		if (type != null) {
			output.writeEnum(type);
			output.writeInt(positionInt);
		}
	}

	@Override
	public void readData(LPDataInput input) {
		super.readData(input);
		if (input.readBoolean()) {
			type = input.readEnum(ModulePositionType.class);
			positionInt = input.readInt();
		}
	}

	public ModuleCoordinatesPacket setModulePos(LogisticsModule module) {
		setBlockPos(module.getPipe().getPos());
		return this;
	}

	public ModuleCoordinatesPacket setPacketPos(ModuleCoordinatesPacket packet) {
		type = packet.type;
		positionInt = packet.positionInt;
		super.setPacketPos(packet);
		return this;
	}

	@SuppressWarnings("unchecked")
	public <T> T getLogisticsModule(EntityPlayer player, Class<T> clazz) {
		LogisticsModule module;
		if (type == ModulePositionType.IN_PIPE) {
			moduleBased = true;
			LogisticsTileGenericPipe pipe = this.getPipe(player.getEntityWorld());
			moduleBased = false;
			if (pipe == null || !(pipe.pipe instanceof CoreRoutedPipe)) {
				throw new TargetNotFoundException("Couldn't find " + clazz.getName() + ", pipe didn't exsist", this);
			}
			module = ((CoreRoutedPipe) pipe.pipe).getLogisticsModule();
		} else if (type == ModulePositionType.IN_HAND) {
			if (MainProxy.isServer(player.getEntityWorld())) {
				if (player.openContainer instanceof DummyModuleContainer) {
					DummyModuleContainer dummy = (DummyModuleContainer) player.openContainer;
					module = dummy.getModule();
				} else {
					throw new TargetNotFoundException("Couldn't find " + clazz.getName() + ", container wasn't a DummyModule Container", this);
				}
			} else {
				module = MainProxy.proxy.getModuleFromGui();
				if (module == null) {
					throw new TargetNotFoundException("Couldn't find " + clazz.getName() + ", GUI didn't provide the module", this);
				}
			}
		} else {
			moduleBased = true;
			LogisticsTileGenericPipe pipe = this.getPipe(player.getEntityWorld());
			moduleBased = false;
			if (pipe == null || !(pipe.pipe instanceof CoreRoutedPipe)) {
				throw new TargetNotFoundException("Couldn't find " + clazz.getName() + ", pipe didn't exsist", this);
			} else if (!pipe.isInitialized()) {
				return null;
			}
			if (!(pipe.pipe instanceof PipeLogisticsChassi)) {
				throw new TargetNotFoundException("Couldn't find " + clazz.getName() + ", pipe wasn't a chassi pipe", this);
			}
			module = ((PipeLogisticsChassi) pipe.pipe).getSubModule(positionInt);
		}
		if (module != null) {
			if (!(clazz.isAssignableFrom(module.getClass()))) {
				throw new TargetNotFoundException("Couldn't find " + clazz.getName() + ", found " + module.getClass(), this);
			}
		} else {
			throw new TargetNotFoundException("Couldn't find " + clazz.getName(), this);
		}
		return (T) module;
	}

	@Override
	public <T> T getTileAs(World world, Class<T> clazz) {
		if (LogisticsPipes.isDEBUG() && !moduleBased && type != null) {
			new Exception("ModulePacket was asked for a pipe").printStackTrace();
		}
		return super.getTileAs(world, clazz);
	}
}
