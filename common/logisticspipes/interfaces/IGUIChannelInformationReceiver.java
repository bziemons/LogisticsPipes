package logisticspipes.interfaces;

import network.rs485.logisticspipes.routing.ChannelInformation;

public interface IGUIChannelInformationReceiver {

	void handleChannelInformation(ChannelInformation channel, boolean flag);
}
