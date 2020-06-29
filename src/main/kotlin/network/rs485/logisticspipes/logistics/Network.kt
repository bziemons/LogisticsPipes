/*
 * Copyright (c) 2020  RS485
 *
 * "LogisticsPipes" is distributed under the terms of the Minecraft Mod Public
 * License 1.0.1, or MMPL. Please check the contents of the license located in
 * https://github.com/RS485/LogisticsPipes/blob/dev/LICENSE.md
 *
 * This file can instead be distributed under the license terms of the
 * MIT license:
 *
 * Copyright (c) 2020  RS485
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

package network.rs485.logisticspipes.logistics

import logisticspipes.LogisticsPipes
import logisticspipes.routing.ServerRouter
import logisticspipes.utils.SinkReply
import logisticspipes.utils.item.ItemIdentifier
import net.minecraft.item.ItemStack
import network.rs485.logisticspipes.api.LogisticsNetwork
import java.util.*

private typealias InterestCallback = (stack: ItemStack, itemid: ItemIdentifier, bestPriority: Int, bestCustomPriority: Int, allowDefault: Boolean, includeInTransit: Boolean, forcePassive: Boolean) -> SinkReply

class Network : LogisticsNetwork {
    companion object {
        @JvmStatic
        @Deprecated("global state")
        val defaultNetwork = Network()
    }

    private val routersWithInterest: TreeMap<ServerRouter, InterestCallback> = TreeMap()
    private val routers: TreeSet<ServerRouter> = TreeSet()

    fun addEdge(router: ServerRouter) {
        routers.add(router)
    }

    fun removeEdge(router: ServerRouter) {
        routers.remove(router)
        routersWithInterest.remove(router)
    }

    fun registerInterest(router: ServerRouter, callback: InterestCallback) {
        if (LogisticsPipes.isDEBUG()) {
            if (router in routersWithInterest) error("Overriding existing entry in routersWithInterest")
        }
        routersWithInterest[router] = callback
    }

}
