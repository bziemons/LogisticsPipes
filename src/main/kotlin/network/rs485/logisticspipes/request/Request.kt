/*
 * Copyright (c) 2019  RS485
 *
 * "LogisticsPipes" is distributed under the terms of the Minecraft Mod Public
 * License 1.0.1, or MMPL. Please check the contents of the license located in
 * https://github.com/RS485/LogisticsPipes/blob/dev/LICENSE.md
 *
 * This file can instead be distributed under the license terms of the
 * MIT license:
 *
 * Copyright (c) 2019  RS485
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

package network.rs485.logisticspipes.request

import logisticspipes.interfaces.routing.IFilter
import logisticspipes.interfaces.routing.IProvide
import logisticspipes.pipes.basic.CoreRoutedPipe
import logisticspipes.proxy.SimpleServiceLocator
import logisticspipes.request.RequestTree
import logisticspipes.request.resources.IResource
import logisticspipes.routing.ExitRoute
import logisticspipes.routing.IRouter
import logisticspipes.routing.PipeRoutingConnectionType
import logisticspipes.routing.ServerRouter
import logisticspipes.utils.item.ItemIdentifier
import java.util.*


class Request(private val resource: IResource,
              private val parent: Request? = null) {
    companion object {
        private fun providers(item: ItemIdentifier, destination: IRouter): Sequence<ExitRoute> {
            val interestedRouterBitSet = ServerRouter.getRoutersInterestedIn(item)
            var routerIndex = interestedRouterBitSet.nextSetBit(0)

            return generateSequence {
                SimpleServiceLocator.routerManager.getRouterUnsafe(routerIndex, false).takeUnless {
                    routerIndex == -1
                }?.apply {
                    routerIndex = interestedRouterBitSet.nextSetBit(routerIndex + 1)
                }
            }.filter {
                it.isValidCache
            }.flatMap { it.getDistanceTo(destination).asSequence() }
        }
    }

//    var delivered = resource.clone(0)
//    var children = LinkedList<Request>()

    private fun providerPipes(): Sequence<Pair<CoreRoutedPipe, LinkedList<IFilter>>> {
        return providers(resource.asItem, resource.router)
                .filter { it.containsFlag(PipeRoutingConnectionType.canRequestFrom) }
                .sortedWith(RequestTree.workWeightedSorter(1.0))
                .filter { it.destination.pipe is IProvide }
                .map { it.destination.pipe to LinkedList(it.filters) }
    }

    fun checkProvider(isDone: () -> Boolean,
                      canProvide: (provider: IProvide, list: LinkedList<IFilter>) -> Unit): Boolean {
        val thisPipe = resource.router.cachedPipe ?: return false
        providerPipes().forEach {
            if (isDone()) return true
            if (it.first.router != null && it.first.router.pipe != null) {
                if (!thisPipe.sharesInterestWith(it.first.router.pipe)) {
                    canProvide(it.first as IProvide, it.second)
                }
            }
        }
        return isDone()
    }
}