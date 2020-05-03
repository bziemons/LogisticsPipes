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

import logisticspipes.interfaces.routing.IRequestItems
import logisticspipes.pipes.PipeItemsRequestLogistics
import logisticspipes.request.ICraftingTemplate
import logisticspipes.request.IPromise
import logisticspipes.request.RequestTree
import logisticspipes.request.RequestTreeNode
import logisticspipes.request.resources.IResource
import logisticspipes.request.resources.ItemResource
import logisticspipes.routing.ExitRoute
import logisticspipes.routing.IRouter
import logisticspipes.routing.PipeRoutingConnectionType
import logisticspipes.routing.ServerRouter
import logisticspipes.utils.item.ItemIdentifierStack
import net.minecraft.init.Blocks
import net.minecraft.init.Bootstrap
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.util.EnumFacing
import network.rs485.logisticspipes.api.IRouterProvider
import java.util.*
import org.junit.jupiter.api.*

internal class RequestTest {

    @BeforeEach
    fun setUp() {
        Bootstrap.register()
    }

    @AfterEach
    fun tearDown() {
    }

    @Test
    fun `test simple addPromisesInOrder run`() {
        val itemStack = ItemStack(Blocks.PLANKS)
        val stack = ItemIdentifierStack.getFromStack(itemStack)
        val resource = MockItemResource(stack, MockRequester(itemStack.item))
        val routerProvider = MockRouterProvider(emptyList())
        MockRequestTreeNode(
                id = "node1",
                resource = resource,
                requestFlags = RequestTree.defaultRequestFlags,
                routerProvider = routerProvider
        ).apply {
            getRequest().addPromisesInOrder(
                    node = this,
                    requestFlags = RequestTree.defaultRequestFlags,
                    root = getRoot(),
                    isCrafterUsed = this::isCrafterUsed,
                    getSubRequests = this::getSubRequests)
            // no error? great!
        }
    }

    @Test
    fun `test addPromisesInOrder run with provider`() {
        val itemStack = ItemStack(Blocks.PLANKS)
        val stack = ItemIdentifierStack.getFromStack(itemStack)
        val requester = MockRequester(itemStack.item)
        val resource = MockItemResource(stack, requester)
        val mockedProviderRouter = ServerRouter(UUID.randomUUID(), 0, 0, 1, 0)
        val routerProvider = MockRouterProvider(
                listOf(makeExitRoute(
                        source = mockedProviderRouter,
                        destination = requester.mockedRouter,
                        exitOrientation = EnumFacing.UP,
                        insertOrientation = EnumFacing.DOWN,
                        metric = 1.0,
                        connectionDetails = EnumSet.allOf(PipeRoutingConnectionType::class.java),
                        blockDistance = 1)
                )
        )
        MockRequestTreeNode(
                id = "node1",
                resource = resource,
                requestFlags = RequestTree.defaultRequestFlags,
                routerProvider = routerProvider
        ).apply {
            getRequest().addPromisesInOrder(
                    node = this,
                    requestFlags = RequestTree.defaultRequestFlags,
                    root = getRoot(),
                    isCrafterUsed = this::isCrafterUsed,
                    getSubRequests = this::getSubRequests)
            // no error? great!
        }
    }

    private fun makeExitRoute(source: ServerRouter, destination: ServerRouter, exitOrientation: EnumFacing, insertOrientation: EnumFacing, metric: Double, connectionDetails: EnumSet<PipeRoutingConnectionType>, blockDistance: Int): ExitRoute {
        return ExitRoute(source, destination, exitOrientation, insertOrientation, metric, connectionDetails, blockDistance)
    }
}

private class MockRequester(item: Item) : PipeItemsRequestLogistics(item) {
    val mockedRouter = ServerRouter(UUID.randomUUID(), 0, 0, 0, 0)

    override fun getRouter(): IRouter {
        return mockedRouter
    }
}

private class MockItemResource(stack: ItemIdentifierStack, requester: IRequestItems) : ItemResource(stack, requester) {
    override fun toString(): String {
        return "MockItemResource{${super.getItem()}}"
    }
}

private class MockRouterProvider(val exitRoutes: List<ExitRoute>) : IRouterProvider {
    override fun getInterestedExits(resource: IResource, destination: IRouter): Sequence<ExitRoute> {
        println("[MockRouterProvider] getInterestedExits($resource, $destination): $exitRoutes")
        return exitRoutes.asSequence()
    }
}

private class MockRequestTree(val id: String,
                              resource: IResource,
                              requestFlags: EnumSet<ActiveRequestType>,
                              routerProvider: IRouterProvider) :
        RequestTree(resource, null, requestFlags, null, routerProvider)

private class MockRequestTreeNode(val id: String,
                                  resource: IResource,
                                  requestFlags: EnumSet<RequestTree.ActiveRequestType>,
                                  routerProvider: IRouterProvider) :
        RequestTreeNode(resource,
                MockRequestTree("$id.root", resource, requestFlags, routerProvider),
                requestFlags,
                null,
                routerProvider) {
    fun getRequest(): Request {
        return super.request!!
    }

    fun getRoot(): RequestTree {
        return super.root!!
    }

    override fun addPromise(promise: IPromise?) {
        println("[MockRequestTreeNode.$id] addPromise($promise)")
        super.addPromise(promise)
    }

    public override fun isCrafterUsed(test: ICraftingTemplate?): Boolean {
        val crafterUsed = super.isCrafterUsed(test)
        println("[MockRequestTreeNode.$id] isCrafterUsed($test): $crafterUsed")
        return crafterUsed
    }

    public override fun getSubRequests(nCraftingSets: Int, template: ICraftingTemplate?): Int {
        val subRequests = super.getSubRequests(nCraftingSets, template)
        println("[MockRequestTreeNode.$id] getSubRequests($nCraftingSets, $template): $subRequests")
        return subRequests
    }

    override fun isDone(): Boolean {
        val done = super.isDone()
        println("[MockRequestTreeNode.$id] isDone(): $done")
        return done
    }
}
