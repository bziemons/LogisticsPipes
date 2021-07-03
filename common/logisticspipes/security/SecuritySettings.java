package logisticspipes.security;

import javax.annotation.Nonnull;

import net.minecraft.nbt.CompoundNBT;

import logisticspipes.interfaces.routing.ISaveState;

public class SecuritySettings implements ISaveState {

	public String name;
	public boolean openGui = false;
	public boolean openRequest = false;
	public boolean openUpgrades = false;
	public boolean openNetworkMonitor = false;
	public boolean removePipes = false;
	public boolean accessRoutingChannels = false;

	public SecuritySettings(String name) {
		this.name = name;
	}

	@Override
	public void readFromNBT(@Nonnull CompoundNBT tag) {
		String prev = name;
		name = CompoundNBT.getString("name");
		if (name.equals("")) {
			name = prev;
		}
		openGui = CompoundNBT.getBoolean("openGui");
		openRequest = CompoundNBT.getBoolean("openRequest");
		openUpgrades = CompoundNBT.getBoolean("openUpgrades");
		openNetworkMonitor = CompoundNBT.getBoolean("openNetworkMonitor");
		removePipes = CompoundNBT.getBoolean("removePipes");
		accessRoutingChannels = CompoundNBT.getBoolean("accessRoutingChannels");
	}

	@Override
	public void writeToNBT(@Nonnull CompoundNBT tag) {
		if (name == null || name.isEmpty()) {
			return;
		}
		tag.putString("name", name);
		tag.putBoolean("openGui", openGui);
		tag.putBoolean("openRequest", openRequest);
		tag.putBoolean("openUpgrades", openUpgrades);
		tag.putBoolean("openNetworkMonitor", openNetworkMonitor);
		tag.putBoolean("removePipes", removePipes);
		tag.putBoolean("accessRoutingChannels", accessRoutingChannels);
	}
}
