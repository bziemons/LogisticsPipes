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

package network.rs485.logisticspipes.module

import logisticspipes.interfaces.IInventoryUtil
import logisticspipes.interfaces.IPipeServiceProvider
import logisticspipes.interfaces.ISlotUpgradeManager
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.Direction
import network.rs485.logisticspipes.connection.NeighborTileEntity
import network.rs485.logisticspipes.connection.getInventoryUtil
import network.rs485.logisticspipes.connection.sneakyInsertion
import java.util.*

fun IPipeServiceProvider.availableSneakyInventories(sneakyDirection: Direction?): List<IInventoryUtil?> = sneakyDirection?.let {
    availableAdjacent.inventories().map { adjacent -> adjacent.sneakyInsertion().from(sneakyDirection).getInventoryUtil() }
} ?: availableInventories()

fun IPipeServiceProvider.availableSneakyInventories(upgradeManager: ISlotUpgradeManager): List<IInventoryUtil?> =
    availableAdjacent.inventories().map { adjacent -> adjacent.sneakyInsertion().from(upgradeManager).getInventoryUtil() }

fun IPipeServiceProvider.availableInventories(): List<IInventoryUtil?> =
    availableAdjacent.inventories().map(NeighborTileEntity<TileEntity>::getInventoryUtil)
