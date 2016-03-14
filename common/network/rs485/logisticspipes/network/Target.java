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

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.entity.player.EntityPlayerMP;

import cpw.mods.fml.common.network.FMLEmbeddedChannel;
import cpw.mods.fml.common.network.FMLOutboundHandler;
import cpw.mods.fml.common.network.NetworkRegistry.TargetPoint;
import cpw.mods.fml.relauncher.Side;

import static cpw.mods.fml.relauncher.Side.CLIENT;
import static cpw.mods.fml.relauncher.Side.SERVER;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFutureListener;
import io.netty.util.AttributeMap;

abstract class Target {

	public static final Target TO_ALL_CLIENTS = new Target(CLIENT) {

		@Override
		protected void setTarget(AttributeMap attributeMap) {
			attributeMap.attr(FMLOutboundHandler.FML_MESSAGETARGET).set(FMLOutboundHandler.OutboundTarget.ALL);
		}

		@Override
		public int hashCode() {
			return 1;
		}
	};
	public static final Target TO_SERVER = new Target(SERVER) {

		@Override
		protected void setTarget(AttributeMap attributeMap) {
			attributeMap.attr(FMLOutboundHandler.FML_MESSAGETARGET).set(FMLOutboundHandler.OutboundTarget.TOSERVER);
		}

		@Override
		public int hashCode() {
			return 2;
		}
	};
	private final Side originSide;

	protected Target(Side targetSide) {
		// convert from target to origin side
		originSide = targetSide == SERVER ? CLIENT : SERVER;
	}

	protected abstract void setTarget(AttributeMap attributeMap);

	public void sendBytes(ByteBuf bytes) {
		FMLEmbeddedChannel embeddedChannel = LPChannel.getInstance().getSidedChannels().get(originSide);
		setTarget(embeddedChannel);
		embeddedChannel.writeAndFlush(bytes).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
	}

	static class PlayerTarget extends Target {

		private static final Map<EntityPlayerMP, Target> playerTargetMap = new WeakHashMap<>();
		private EntityPlayerMP player;

		public PlayerTarget(EntityPlayerMP player) {
			super(CLIENT);
			this.player = player;
		}

		public static Target get(EntityPlayerMP player) {
			if (playerTargetMap.containsKey(player)) {
				return playerTargetMap.get(player);
			} else {
				Target target = new PlayerTarget(player);
				playerTargetMap.put(player, target);
				return target;
			}
		}

		@Override
		protected void setTarget(AttributeMap attributeMap) {
			attributeMap.attr(FMLOutboundHandler.FML_MESSAGETARGET).set(FMLOutboundHandler.OutboundTarget.PLAYER);
			attributeMap.attr(FMLOutboundHandler.FML_MESSAGETARGETARGS).set(player);
		}

		@Override
		public int hashCode() {
			return player.hashCode();
		}
	}

	static class PointTarget extends Target {

		private static final Map<TargetPoint, Target> targetPointMap = new WeakHashMap<>();
		private TargetPoint targetPoint;

		protected PointTarget(TargetPoint point) {
			super(CLIENT);
			this.targetPoint = point;
		}

		public static Target get(TargetPoint point) {
			if (targetPointMap.containsKey(point)) {
				return targetPointMap.get(point);
			} else {
				Target target = new PointTarget(point);
				targetPointMap.put(point, target);
				return target;
			}
		}

		@Override
		protected void setTarget(AttributeMap attributeMap) {
			attributeMap.attr(FMLOutboundHandler.FML_MESSAGETARGET).set(FMLOutboundHandler.OutboundTarget.ALLAROUNDPOINT);
			attributeMap.attr(FMLOutboundHandler.FML_MESSAGETARGETARGS).set(targetPoint);
		}

		@Override
		public int hashCode() {
			return targetPoint.hashCode();
		}
	}

	static class DimensionTarget extends Target {

		private static final Map<Integer, Target> dimensionTargetMap = new HashMap<>();
		private int dimension;

		public DimensionTarget(int dimension) {
			super(CLIENT);
			this.dimension = dimension;
		}

		public static Target get(int dimension) {
			if (dimensionTargetMap.containsKey(dimension)) {
				return dimensionTargetMap.get(dimension);
			} else {
				Target target = new DimensionTarget(dimension);
				dimensionTargetMap.put(dimension, target);
				return target;
			}
		}

		@Override
		protected void setTarget(AttributeMap attributeMap) {
			attributeMap.attr(FMLOutboundHandler.FML_MESSAGETARGET).set(FMLOutboundHandler.OutboundTarget.DIMENSION);
			attributeMap.attr(FMLOutboundHandler.FML_MESSAGETARGETARGS).set(dimension);
		}

		@Override
		public int hashCode() {
			return dimension;
		}
	}
}
