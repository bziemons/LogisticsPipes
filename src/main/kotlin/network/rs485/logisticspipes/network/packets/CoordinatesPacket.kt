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

import logisticspipes.network.exception.TargetNotFoundException
import logisticspipes.pipes.basic.LogisticsTileGenericPipe
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import network.rs485.logisticspipes.network.Packet
import network.rs485.logisticspipes.util.LPDataInput
import network.rs485.logisticspipes.util.LPDataOutput
import network.rs485.logisticspipes.world.DoubleCoordinates
import java.util.function.Function
import javax.annotation.Nonnull

abstract class CoordinatesPacket : Packet {
    var posX: Int = 0
    var posY: Int = 0
    var posZ: Int = 0

    var blockPos: BlockPos
        get() = BlockPos(posX, posY, posZ)
        set(value) {
            posX = value.x
            posY = value.y
            posZ = value.z
        }

    override fun writeData(output: LPDataOutput) {
        output.writeInt(posX)
        output.writeInt(posY)
        output.writeInt(posZ)
    }

    override fun readData(input: LPDataInput) {
        posX = input.readInt()
        posY = input.readInt()
        posZ = input.readInt()
    }

    fun setTilePos(tile: TileEntity): CoordinatesPacket {
        posX = tile.pos.x
        posY = tile.pos.y
        posZ = tile.pos.z
        return this
    }

    fun setLPPos(pos: DoubleCoordinates): CoordinatesPacket {
        posX = pos.xInt
        posY = pos.yInt
        posZ = pos.zInt
        return this
    }

    fun setPacketPos(packet: CoordinatesPacket): CoordinatesPacket {
        posX = packet.posX
        posY = packet.posY
        posZ = packet.posZ
        return this
    }

    fun getTileAs(world: World?, validateResult: Function<TileEntity?, Boolean?>): TileEntity {
        val tile = getTileAs(world, TileEntity::class.java)
        if (!validateResult.apply(tile)!!) {
            throw TargetNotFoundException("TileEntity condition not met", this)
        }
        return tile
    }

    /**
     * Retrieves tileEntity at packet coordinates if any.
     */
    open fun <T> getTileAs(world: World?, clazz: Class<T>): T {
        return getTileAs(this, world, BlockPos(posX, posY, posZ), clazz)
    }

    /**
     * Retrieves tileEntity or CoreUnroutedPipe at packet coordinates if any.
     */
    fun <T: TileEntity> getTileOrPipe(world: World?, clazz: Class<T>): T {
        val tile = getWorldTile(this, world, BlockPos(posX, posY, posZ))
        if (tile != null) {
            if (clazz.isAssignableFrom(tile.javaClass)) {
                @Suppress("UNCHECKED_CAST")
                return tile as T
            }
            if (tile is LogisticsTileGenericPipe) {
                if (tile.pipe != null && clazz.isAssignableFrom(tile.pipe.javaClass)) {
                    @Suppress("UNCHECKED_CAST")
                    return tile.pipe as T
                }
                throw TargetNotFoundException(
                    "Couldn't find " + clazz.name + ", found pipe with " + tile.javaClass + " at: " + BlockPos(
                        posX, posY, posZ
                    ), this
                )
            }
        } else {
            throw TargetNotFoundException("Couldn't find " + clazz.name + " at: " + BlockPos(posX, posY, posZ), this)
        }
        throw TargetNotFoundException(
            "Couldn't find " + clazz.name + ", found " + tile.javaClass + " at: " + BlockPos(
                posX, posY, posZ
            ), this
        )
    }

    /**
     * Retrieves pipe at packet coordinates if any.
     */
    @Deprecated("Use getPipe with LTGPCompletionCheck", ReplaceWith(
        "getPipe(world, LTGPCompletionCheck.NONE)",
        "logisticspipes.network.abstractpackets.CoordinatesPacket.LTGPCompletionCheck"
    )
    )
    fun getPipe(world: World?): LogisticsTileGenericPipe {
        return getPipe(world, LTGPCompletionCheck.NONE)
    }

    @Nonnull
    fun getPipe(world: World?, check: LTGPCompletionCheck): LogisticsTileGenericPipe {
        val pipe = getTileAs(world, LogisticsTileGenericPipe::class.java)
        if (check == LTGPCompletionCheck.PIPE || check == LTGPCompletionCheck.TRANSPORT) {
            if (pipe.pipe == null) {
                throw TargetNotFoundException("The found pipe didn't have a loaded pipe field", this)
            }
        }
        if (check == LTGPCompletionCheck.TRANSPORT) {
            if (pipe.pipe.transport == null) {
                throw TargetNotFoundException("The found pipe didn't have a loaded transport field", this)
            }
        }
        return pipe
    }

    override fun toString(): String {
        return "CoordinatesPacket(posX=$posX, posY=$posY, posZ=$posZ)"
    }

    enum class LTGPCompletionCheck {
        NONE, PIPE, TRANSPORT
    }

    companion object {
        @JvmStatic
		@Nonnull
        fun <T> getTileAs(whosAsking: Any, world: World?, blockPos: BlockPos, clazz: Class<T>): T {
            val tile = getWorldTile(whosAsking, world, blockPos)
            if (tile != null) {
                if (clazz.isAssignableFrom(tile.javaClass)) {
                    @Suppress("UNCHECKED_CAST")
                    return tile as T
                }
                throw TargetNotFoundException(
                    "Couldn't find " + clazz.name + ", found " + tile.javaClass + " at: " + blockPos,
                    whosAsking
                )
            } else {
                throw TargetNotFoundException("Couldn't find " + clazz.name + " at: " + blockPos, whosAsking)
            }
        }

        private fun getWorldTile(whosAsking: Any, world: World?, blockPos: BlockPos): TileEntity? {
            if (world == null) {
                throw TargetNotFoundException("World was null", whosAsking)
            }
            if (world.isAirBlock(blockPos)) {
                throw TargetNotFoundException("Only found air at: $blockPos", whosAsking)
            }
            return world.getTileEntity(blockPos)
        }
    }
}