/*
 * Copyright (c) 2021  RS485
 *
 * "LogisticsPipes" is distributed under the terms of the Minecraft Mod Public
 * License 1.0.1, or MMPL. Please check the contents of the license located in
 * https://github.com/RS485/LogisticsPipes/blob/dev/LICENSE.md
 *
 * This file can instead be distributed under the license terms of the
 * MIT license:
 *
 * Copyright (c) 2021  RS485
 *
 * This MIT license was reworded to only match this file. If you use the regular
 * MIT license in your project, replace this copyright notice (this line and any
 * lines below and NOT the copyright line above) with the lines from the original
 * MIT license located here: http://opensource.org/licenses/MIT
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this file and associated documentation files (the "Source Code"), to deal in
 * the Source Code without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Source Code, and to permit persons to whom the Source Code is furnished
 * to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Source Code, which also can be
 * distributed under the MIT.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package network.rs485.logisticspipes.network

import logisticspipes.LPConstants
import logisticspipes.LogisticsPipes
import logisticspipes.network.exception.TargetNotFoundException
import net.minecraft.entity.player.ServerPlayerEntity
import net.minecraft.network.PacketBuffer
import net.minecraft.util.ResourceLocation
import net.minecraftforge.fml.network.NetworkEvent
import net.minecraftforge.fml.network.NetworkRegistry
import net.minecraftforge.fml.network.PacketDistributor
import net.minecraftforge.fml.network.simple.SimpleChannel.MessageBuilder.ToBooleanBiFunction
import network.rs485.logisticspipes.network.packets.CAddNewChannelPacket
import network.rs485.logisticspipes.network.packets.CEditChannelPacket
import network.rs485.logisticspipes.network.packets.SActivateNBTDebug
import network.rs485.logisticspipes.util.LPDataIOWrapper
import java.util.function.Supplier

object DefaultChannel {
    private val channelResource = ResourceLocation(LPConstants.LP_MOD_ID, javaClass.name)
    private val fmlChannel = NetworkRegistry.newSimpleChannel(
        channelResource, ::networkProtocolVersion, ::acceptsVersion, ::acceptsVersion
    )

    private val networkProtocolVersion
        get() = "1.0"

    private fun acceptsVersion(version: String?): Boolean = networkProtocolVersion == version

    private fun <T : Packet> registerPacket(packetIndex: Int, packetType: Class<T>, packetFactory: PacketFactory<T>) {
        fmlChannel.messageBuilder(packetType, packetIndex)
            .encoder { packet: Packet, buffer: PacketBuffer ->
                LPDataIOWrapper.writeData(buffer) { packet.writeData(it) }
            }
            .decoder(packetFactory::createPacket)
            .consumer(ToBooleanBiFunction { packet: Packet, contextSupplier: Supplier<NetworkEvent.Context> ->
                try {
                    packet.processPacket(contextSupplier)
                } catch (e: TargetNotFoundException) {
                    if (LogisticsPipes.isDEBUG()) {
                        LogisticsPipes.getLOGGER().error("Packet error in ${packet.javaClass.name}, $packet", e)
                    }
                    false
                }
            })
            .add()
    }

    fun registerPackets() {
        registerPacket(1, SActivateNBTDebug::class.java, ::SActivateNBTDebug)
        registerPacket(2, CAddNewChannelPacket::class.java, ::CAddNewChannelPacket)
        registerPacket(3, CEditChannelPacket::class.java, ::CEditChannelPacket)
    }

    fun <T : Packet> sendPacketToPlayer(packet: T, playerSupplier: Supplier<ServerPlayerEntity>) =
        fmlChannel.send(PacketDistributor.PLAYER.with(playerSupplier), packet)

    fun <T : Packet> sendPacketToServer(packet: T) = fmlChannel.sendToServer(packet)

}
