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

package network.rs485.logisticspipes.network.packets

import logisticspipes.modules.LogisticsModule.ModulePositionType
import network.rs485.logisticspipes.util.LPDataOutput
import network.rs485.logisticspipes.util.LPDataInput
import logisticspipes.modules.LogisticsModule
import net.minecraft.entity.player.PlayerEntity
import logisticspipes.pipes.basic.CoreRoutedPipe
import logisticspipes.network.exception.TargetNotFoundException
import logisticspipes.proxy.MainProxy
import logisticspipes.utils.gui.DummyModuleContainer
import logisticspipes.pipes.PipeLogisticsChassis
import net.minecraft.world.World
import logisticspipes.LogisticsPipes
import java.lang.Exception

abstract class ModuleCoordinatesPacket : CoordinatesPacket() {
    var type: ModulePositionType? = null
    var positionInt = 0

    private var moduleBased = false

    override fun writeData(output: LPDataOutput) {
        super.writeData(output)
        output.writeBoolean(type != null)
        if (type != null) {
            output.writeEnum(type)
            output.writeInt(positionInt)
        }
    }

    override fun readData(input: LPDataInput) {
        super.readData(input)
        if (input.readBoolean()) {
            type = input.readEnum(ModulePositionType::class.java)
            positionInt = input.readInt()
        }
    }

    fun setModulePos(module: LogisticsModule): ModuleCoordinatesPacket {
        type = module.slot
        positionInt = module.positionInt
        if (type?.isInWorld == true) {
            blockPos = module.blockPos
        }
        return this
    }

    fun setPacketPos(packet: ModuleCoordinatesPacket): ModuleCoordinatesPacket {
        type = packet.type
        positionInt = packet.positionInt
        super.setPacketPos(packet)
        return this
    }

    fun <T> getLogisticsModule(player: PlayerEntity, clazz: Class<T>): T? {
        val module: LogisticsModule?
        if (type == ModulePositionType.IN_PIPE) {
            moduleBased = true
            val pipe = this.getPipe(player.entityWorld, LTGPCompletionCheck.NONE)
            moduleBased = false
            if (pipe.pipe !is CoreRoutedPipe) {
                throw TargetNotFoundException("Couldn't find " + clazz.name + ", pipe didn't exsist", this)
            }
            module = (pipe.pipe as CoreRoutedPipe).logisticsModule
        } else if (type == ModulePositionType.IN_HAND) {
            if (MainProxy.isServer(player.entityWorld)) {
                module = if (player.openContainer is DummyModuleContainer) {
                    val dummy = player.openContainer as DummyModuleContainer
                    dummy.module
                } else {
                    throw TargetNotFoundException(
                        "Couldn't find " + clazz.name + ", container wasn't a DummyModule Container",
                        this
                    )
                }
            } else {
                module = MainProxy.proxy.moduleFromGui
                if (module == null) {
                    throw TargetNotFoundException(
                        "Couldn't find " + clazz.name + ", GUI didn't provide the module",
                        this
                    )
                }
            }
        } else {
            moduleBased = true
            val pipe = this.getPipe(player.entityWorld, LTGPCompletionCheck.NONE)
            moduleBased = false
            if (pipe.pipe !is CoreRoutedPipe) {
                throw TargetNotFoundException("Couldn't find " + clazz.name + ", pipe didn't exsist", this)
            } else if (!pipe.isInitialized) {
                return null
            }
            if (pipe.pipe !is PipeLogisticsChassis) {
                throw TargetNotFoundException("Couldn't find " + clazz.name + ", pipe wasn't a chassi pipe", this)
            }
            module = (pipe.pipe as PipeLogisticsChassis).getSubModule(positionInt)
        }
        if (module == null) throw TargetNotFoundException("Couldn't find " + clazz.name, this)
        if (!clazz.isAssignableFrom(module.javaClass)) {
            throw TargetNotFoundException("Couldn't find " + clazz.name + ", found " + module.javaClass, this)
        }
        @Suppress("UNCHECKED_CAST")
        return module as T
    }

    override fun <T> getTileAs(world: World?, clazz: Class<T>): T {
        if (LogisticsPipes.isDEBUG() && !moduleBased && type != null) {
            Exception("ModulePacket was asked for a pipe").printStackTrace()
        }
        return super.getTileAs(world, clazz)
    }

}
