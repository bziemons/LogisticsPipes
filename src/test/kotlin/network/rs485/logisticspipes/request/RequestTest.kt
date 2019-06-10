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

import logisticspipes.pipes.PipeItemsRequestLogistics
import logisticspipes.request.ICraftingTemplate
import logisticspipes.request.IPromise
import logisticspipes.request.RequestTree
import logisticspipes.request.RequestTreeNode
import logisticspipes.request.resources.IResource
import logisticspipes.request.resources.ItemResource
import logisticspipes.routing.ExitRoute
import logisticspipes.routing.IRouter
import logisticspipes.routing.ServerRouter
import logisticspipes.utils.item.ItemIdentifierStack
import net.minecraft.init.Blocks
import net.minecraft.init.Bootstrap
import net.minecraft.item.ItemStack
import network.rs485.logisticspipes.api.IRouterProvider
import java.util.*

internal class RequestTest {

    private var node: MockRequestTreeNode? = null
    private var createdRequest: Request? = null

    @org.junit.jupiter.api.BeforeEach
    fun setUp() {
        Bootstrap.register()
        val stack = ItemIdentifierStack.getFromStack(ItemStack(Blocks.PLANKS))
        val resource = MockItemResource(stack)
        val routerProvider = MockRouterProvider()
        node = MockRequestTreeNode("node1", resource, RequestTree.defaultRequestFlags, routerProvider)
        createdRequest = node!!.getRequest()
    }

    @org.junit.jupiter.api.AfterEach
    fun tearDown() {
        node = null
        createdRequest = null
    }

    @org.junit.jupiter.api.Test
    fun `test addPromisesInOrder`() {
        createdRequest!!.also { request ->
            node!!.apply {
                request.addPromisesInOrder(this, RequestTree.defaultRequestFlags, getRoot(), this::isCrafterUsed, this::getSubRequests)
            }
        }
    }
}

private class MockRequester : PipeItemsRequestLogistics(null) {
    private val mockedRouter = ServerRouter(UUID.randomUUID(), 0, 0, 0, 0)

    override fun getRouter(): IRouter {
        return mockedRouter
    }
}

private class MockItemResource(stack: ItemIdentifierStack) : ItemResource(stack, MockRequester()) {
    override fun toString(): String {
        return "MockItemResource{${super.getItem()}}"
    }
}

private class MockRouterProvider : IRouterProvider {
    override fun getInterestedExits(resource: IResource, destination: IRouter): Sequence<ExitRoute> {
        println("[MockRouterProvider] getInterestedExits($resource, $destination)")
        return generateSequence {
            null
        }
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
                MockRouterProvider()) {
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
