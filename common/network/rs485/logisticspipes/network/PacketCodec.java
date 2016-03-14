/*
 * Copyright (c) 2015  RS485
 *
 * "LogisticsPipes" is distributed under the terms of the Minecraft Mod Public
 * License 1.0.1, or MMPL. Please check the contents of the license located in
 * https://github.com/RS485/LogisticsPipes/blob/dev/LICENSE.md
 *
 * This file can instead be distributed under the license terms of the MIT license:
 *
 * Copyright (c) 2015  RS485
 *
 * This MIT license was reworded to only match this file. If you use the regular MIT license in your project, replace this copyright notice (this line and any lines below and NOT the copyright line above) with the lines from the original MIT license located here: http://opensource.org/licenses/MIT
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this file and associated documentation files (the "Source Code"), to deal in the Source Code without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Source Code, and to permit persons to whom the Source Code is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Source Code, which also can be distributed under the MIT.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package network.rs485.logisticspipes.network;

import java.util.List;

import network.rs485.logisticspipes.network.exception.PacketRetryException;

import logisticspipes.LPConstants;
import logisticspipes.LogisticsPipes;

import cpw.mods.fml.common.network.internal.FMLProxyPacket;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageCodec;

@Sharable
class PacketCodec extends MessageToMessageCodec<FMLProxyPacket, ByteBuf> {

	private final Receiver receiver;

	PacketCodec() {
		receiver = new Receiver();
	}

	@Override
	protected void encode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out) throws Exception {
		msg.retain();
		out.add(new FMLProxyPacket(msg, LPChannel.CHANNEL_NAME));
	}

	@Override
	protected void decode(ChannelHandlerContext ctx, FMLProxyPacket msg, List<Object> out) throws Exception {
		ByteBuf bytes = msg.payload();
		// the first boolean of the packet always tells, whether it is a single packet
		boolean single = bytes.readBoolean();
		if (single) {
			final int packetId = bytes.readInt();
			final int retries = bytes.readInt();
			final int length = bytes.readInt();
			Class<? extends AbstractPacket> packetClazz = LPChannel.PACKET_MAP.get(packetId);
			PacketBufferManager.getInstance().getInPacketBuffer().readPacketForClass(packetClazz, packet -> {
				try {
					packet.read(bytes.slice(bytes.readerIndex(), length));
				} catch (Exception e) {
					LogisticsPipes.log.error("Error when reading packet " + packetClazz.getSimpleName());
					throw e;
				}

				try {
					packet.processPacket();
				} catch (PacketRetryException e) {
					String followUp = "";
					if (packet.needsRetry()) {
						followUp = "\n  Sent retry packet " + retries;
						// ToDO: Retry(retries++)
					}
					LogisticsPipes.log.debug("Packet " + packetClazz.getSimpleName() + " could not be handled: " + e.getMessage() + followUp);
				} catch (Exception e) {
					LogisticsPipes.log.error("Error when handling packet " + packetClazz.getSimpleName(), e);
					if (LPConstants.DEBUG) {
						StackTraceElement[] debugTrace = AbstractPacket.DEBUG_MAP.get(packet.getDebugId());
						Throwable t = new Throwable("Remote stack trace");
						t.setStackTrace(debugTrace);
						LogisticsPipes.log.debug("Packet was sent from:", t);
					}
					throw e;
				} finally {
					AbstractPacket.DEBUG_MAP.remove(packet.getDebugId());
				}
			});
		} else {
			receiver.addPayload(bytes.slice());
		}
	}
}
