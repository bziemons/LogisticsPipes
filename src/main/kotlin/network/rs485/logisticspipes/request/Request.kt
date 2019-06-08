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

import logisticspipes.interfaces.routing.ICraft
import logisticspipes.interfaces.routing.IFilter
import logisticspipes.interfaces.routing.IProvide
import logisticspipes.proxy.SimpleServiceLocator
import logisticspipes.request.*
import logisticspipes.request.resources.IResource
import logisticspipes.routing.ExitRoute
import logisticspipes.routing.IRouter
import logisticspipes.routing.PipeRoutingConnectionType
import logisticspipes.routing.ServerRouter
import logisticspipes.utils.item.ItemIdentifier
import java.util.*
import logisticspipes.utils.tuples.Pair as LPPair


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


        private fun crafters(iRequestType: IResource, validDestinations: List<ExitRoute>): List<logisticspipes.utils.tuples.Pair<ICraftingTemplate, List<IFilter>>> {
            val crafters = ArrayList<logisticspipes.utils.tuples.Pair<ICraftingTemplate, List<IFilter>>>(validDestinations.size)
            for (r in validDestinations) {
                val pipe = r.destination.pipe
                if (r.containsFlag(PipeRoutingConnectionType.canRequestFrom)) {
                    if (pipe is ICraft) {
                        val craftable = (pipe as ICraft).addCrafting(iRequestType)
                        if (craftable != null) {
                            for (filter in r.filters) {
                                if (filter.isBlocked == filter.isFilteredItem(craftable.resultItem) || filter.blockCrafting()) {
                                    continue
                                }
                            }
                            val list = LinkedList<IFilter>()
                            list.addAll(r.filters)
                            crafters.add(logisticspipes.utils.tuples.Pair(craftable, list))
                        }
                    }
                }
            }
            // don't need to sort, as a sorted list is passed in and List guarantees order preservation
            //		Collections.sort(crafters,new CraftingTemplate.PairPrioritizer());
            return crafters
        }
    }

//    var delivered = resource.clone(0)
//    var children = LinkedList<Request>()

    private fun providerPipes(): Sequence<Pair<IProvide, LinkedList<IFilter>>> {
        return providers(resource.asItem, resource.router)
                .filter { it.containsFlag(PipeRoutingConnectionType.canRequestFrom) }
                .sortedWith(RequestTree.workWeightedSorter(1.0))
                .filter { it.destination.pipe is IProvide }
                .map { it.destination.pipe as IProvide to LinkedList(it.filters) }
    }

    fun checkProvider(isDone: () -> Boolean,
                      canProvide: (provider: IProvide, list: LinkedList<IFilter>) -> Unit): Boolean {
        val thisPipe = resource.router.cachedPipe ?: return false
        providerPipes().forEach {
            if (isDone()) return true
            if (it.first.router != null && it.first.router.pipe != null) {
                if (!thisPipe.sharesInterestWith(it.first.router.pipe)) {
                    canProvide(it.first, it.second)
                }
            }
        }
        return isDone()
    }

    fun checkExtras(root: RequestTree,
                    isDone: () -> Boolean,
                    getMissingAmount: () -> Int,
                    addPromise: (promise: IExtraPromise) -> Unit): Boolean {
        root.getExtrasFor(resource).forEach { extraPromise ->
            if (isDone()) return true
            if (extraPromise.amount != 0) {
                val sources = extraPromise.provider.router.routeTable[resource.router.simpleID]
                val foundSource = sources.firstOrNull {
                    it != null && it.containsFlag(PipeRoutingConnectionType.canRouteTo)
                }
                val foundDestination = resource.router.iRoutersByCost.firstOrNull {
                    it.destination === extraPromise.provider.router && it.containsFlag(PipeRoutingConnectionType.canRequestFrom)
                }
                if (foundSource != null && foundDestination != null) {
                    extraPromise.amount = Math.min(extraPromise.amount, getMissingAmount())
                    addPromise(extraPromise)
                }
            }
        }
        return isDone()
    }

    fun checkCrafting(root: RequestTree,
                      isDone: () -> Boolean,
                      getMissingAmount: () -> Int,
                      isCrafterUsed: (test: ICraftingTemplate) -> Boolean,
                      getSubRequests: (nCraftingSetsNeeded: Int, template: ICraftingTemplate) -> Int,
                      addPromise: (promise: IPromise) -> Unit): Boolean {
        // get all the routers
        val routersIndex = ServerRouter.getRoutersInterestedIn(resource)
        val validSources = ArrayList<ExitRoute>() // get the routing table
        var i = routersIndex.nextSetBit(0)
        while (i >= 0) {
            val r = SimpleServiceLocator.routerManager.getRouterUnsafe(i, false)

            if (!r.isValidCache) {
                i = routersIndex.nextSetBit(i + 1)
                continue //Skip Routers without a valid pipe
            }

            val e = resource.router.getDistanceTo(r)
            if (e != null) {
                validSources.addAll(e)
            }
            i = routersIndex.nextSetBit(i + 1)
        }
        val wSorter = RequestTree.workWeightedSorter(0.0) // distance doesn't matter, because ingredients have to be delivered to the crafter, and we can't tell how long that will take.
        Collections.sort(validSources, wSorter)

        val allCraftersForItem = crafters(resource, validSources)

        // if you have a crafter which can make the top treeNode.getStack().getItem()
        val iterAllCrafters = allCraftersForItem.iterator()

        //a queue to store the crafters, sorted by todo; we will fill up from least-most in a balanced way.
        val craftersSamePriority = PriorityQueue<CraftingSorterNode>(5)
        val craftersToBalance = ArrayList<CraftingSorterNode>()
        //TODO ^ Make this a generic list
        var done = false
        var lastCrafter: LPPair<ICraftingTemplate, List<IFilter>>? = null
        var currentPriority = 0
        outer@ while (!done) {

            /// First: Create a list of all crafters with the same priority (craftersSamePriority).
            if (iterAllCrafters.hasNext()) {
                if (lastCrafter == null) {
                    lastCrafter = iterAllCrafters.next()
                }
            } else if (lastCrafter == null) {
                done = true
            }

            var itemsNeeded = getMissingAmount()

            if (lastCrafter != null && (craftersSamePriority.isEmpty() || currentPriority == lastCrafter.value1.priority)) {
                currentPriority = lastCrafter.value1.priority
                val crafter = lastCrafter
                lastCrafter = null
                val template = crafter.value1
                if (isCrafterUsed(template)) {
                    continue
                }
                if (!template.canCraft(resource)) {
                    continue // we this is crafting something else
                }
                for (filter in crafter.value2) { // is this filtered for some reason.
                    if (filter.isBlocked == filter.isFilteredItem(template.resultItem) || filter.blockCrafting()) {
                        continue@outer
                    }
                }
                val cn = CraftingSorterNode(crafter.value1, itemsNeeded, root, getSubRequests, getMissingAmount, addPromise)
                //				if(cn.getWorkSetsAvailableForCrafting()>0)
                craftersSamePriority.add(cn)
                continue
            }
            if (craftersToBalance.isEmpty() && (craftersSamePriority == null || craftersSamePriority.isEmpty())) {
                continue //nothing at this priority was available for crafting
            }
            /// end of crafter prioriy selection.

            if (craftersSamePriority.size == 1) { // then no need to balance.
                craftersToBalance.add(craftersSamePriority.poll())
                // automatically capped at the real amount of extra work.
                craftersToBalance[0].addToWorkRequest(itemsNeeded)
            } else {
                //				for(CraftingSorterNode c:craftersSamePriority)
                //					c.clearWorkRequest(); // so the max request isn't in there; nothing is reserved, balancing can work correctly.

                // go through this list, pull the crafter(s) with least work, add work until either they can not do more work,
                //   or the amount of work they have is equal to the next-least busy crafter. then pull the next crafter and repeat.
                if (!craftersSamePriority.isEmpty()) {
                    craftersToBalance.add(craftersSamePriority.poll())
                }
                // while we crafters that can work and we have work to do.
                while (!craftersToBalance.isEmpty() && itemsNeeded > 0) {
                    //while there is more, and the next crafter has the same toDo as the current one, add it to craftersToBalance.
                    //  typically pulls 1 at a time, but may pull multiple, if they have the exact same todo.
                    while (!craftersSamePriority.isEmpty() && craftersSamePriority.peek().currentToDo() <= craftersToBalance[0].currentToDo()) {
                        craftersToBalance.add(craftersSamePriority.poll())
                    }

                    // find the most we can add this iteration
                    var cap: Int
                    if (!craftersSamePriority.isEmpty()) {
                        cap = craftersSamePriority.peek().currentToDo()
                    } else {
                        cap = Integer.MAX_VALUE
                    }

                    //split the work between N crafters, up to "cap" (at which point we would be dividing the work between N+1 crafters.
                    val floor = craftersToBalance[0].currentToDo()
                    cap = Math.min(cap, floor + (itemsNeeded + craftersToBalance.size - 1) / craftersToBalance.size)

                    for (crafter in craftersToBalance) {
                        val request = Math.min(itemsNeeded, cap - floor)
                        if (request > 0) {
                            val craftingDone = crafter.addToWorkRequest(request)
                            itemsNeeded -= craftingDone // ignored under-crafting
                        }
                    }

                } // all craftersToBalance exhausted, or work completed.

            }// end of else more than 1 crafter at this priority
            // commit this work set.
            val iter = craftersToBalance.iterator()
            while (iter.hasNext()) {
                val c = iter.next()
                if (c.stacksOfWorkRequested > 0 && !c.addWorkPromisesToTree()) { // then it ran out of resources
                    iter.remove()
                }

            }
            itemsNeeded = getMissingAmount()

            if (itemsNeeded <= 0) {
                break@outer // we have everything we need for this crafting request
            }

            // don't clear, because we might have under-requested, and need to consider these again
            if (!craftersToBalance.isEmpty()) {
                done = false
                //craftersSamePriority.clear(); // we've extracted all we can from these priority crafters, and we still have more to do, back to the top to get the next priority level.
            }
        }
        //LogisticsPipes.log.info("done");
        return isDone()
    }

    private inner class CraftingSorterNode internal constructor(
            val crafter: ICraftingTemplate,
            maxCount: Int,
            tree: RequestTree,
            private val getSubRequests: (nCraftingSetsNeeded: Int, template: ICraftingTemplate) -> Int,
            private val getMissingAmount: () -> Int,
            private val addPromise: (promise: IPromise) -> Unit
    ) : Comparable<CraftingSorterNode> {
        val originalToDo: Int = crafter.crafter.todo
        private val setSize: Int = crafter.resultStackSize
        private val maxWorkSetsAvailable: Int = (getMissingAmount() + setSize - 1) / setSize

        var stacksOfWorkRequested: Int = 0
            private set

        internal fun calculateMaxWork(maxSetsToCraft: Int): Int {

            val nCraftingSetsNeeded: Int
            if (maxSetsToCraft == 0) {
                nCraftingSetsNeeded = (getMissingAmount() + setSize - 1) / setSize
            } else {
                nCraftingSetsNeeded = maxSetsToCraft
            }

            if (nCraftingSetsNeeded == 0) {
                return 0
            }

            val template = crafter

            return getSubRequests(nCraftingSetsNeeded, template)
        }

        internal fun addToWorkRequest(extraWork: Int): Int {
            val stacksRequested = (extraWork + setSize - 1) / setSize
            stacksOfWorkRequested += stacksRequested
            return stacksRequested * setSize
        }

        /**
         * Add promises for the requested work to the tree.
         */
        internal fun addWorkPromisesToTree(): Boolean {
            val template = crafter
            val setsToCraft = Math.min(stacksOfWorkRequested, maxWorkSetsAvailable)
            val setsAbleToCraft = calculateMaxWork(setsToCraft) // Deliberately outside the 0 check, because calling generatePromies(0) here clears the old ones.

            if (setsAbleToCraft > 0) { // sanity check, as creating 0 sized promises is an exception. This should never be hit.
                //if we got here, we can at least some of the remaining amount
                val job = template.generatePromise(setsAbleToCraft)
                if (job.getAmount() != setsAbleToCraft * setSize) {
                    throw IllegalStateException("generatePromises not creating the promisesPromised; this is goign to end badly.")
                }
                addPromise(job)
            }
            val isDone = setsToCraft == setsAbleToCraft
            stacksOfWorkRequested = 0 // just incase we call it twice.
            return isDone
        }

        override fun compareTo(o: CraftingSorterNode): Int {
            return currentToDo() - o.currentToDo()
        }

        fun currentToDo(): Int {
            return originalToDo + stacksOfWorkRequested * setSize
        }
    }

}
