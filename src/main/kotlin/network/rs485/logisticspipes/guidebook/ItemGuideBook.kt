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

package network.rs485.logisticspipes.guidebook

import logisticspipes.items.LogisticsItem
import logisticspipes.network.PacketHandler
import logisticspipes.network.guis.OpenGuideBook
import logisticspipes.proxy.MainProxy
import net.minecraft.client.Minecraft
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.inventory.EquipmentSlotType
import net.minecraft.item.ItemStack
import net.minecraft.nbt.CompoundNBT
import net.minecraft.nbt.ListNBT
import net.minecraft.util.ActionResult
import net.minecraft.util.ActionResultType
import net.minecraft.util.Hand
import net.minecraft.world.World
import network.rs485.logisticspipes.gui.guidebook.GuiGuideBook
import network.rs485.logisticspipes.gui.guidebook.IPageData
import network.rs485.logisticspipes.gui.guidebook.PageData
import network.rs485.logisticspipes.gui.guidebook.Page
import network.rs485.logisticspipes.network.packets.SetCurrentPagePacket

class ItemGuideBook : LogisticsItem() {

    companion object {
        /**
         * Loads the current page and the tabs from the stack's NBT. Returns Pair(currentPage, Tabs)
         */
        private fun loadDataFromNBT(stack: ItemStack): Pair<PageData, List<PageData>> {
            var currentPage: PageData? = null
            var tabPages: List<PageData>? = null
            if (stack.hasTag()) {
                val nbt = stack.tag!!
                if (nbt.contains("version")) {
                    when (nbt.getByte("version")) {
                        1.toByte() -> {
                            currentPage = PageData(nbt.getCompound("page"))
                            // type 10 = CompoundNBT, see net.minecraft.nbt.NBTBase.createNewByType
                            val tagList = nbt.getList("bookmarks", 10)
                            tabPages = tagList.mapNotNull { tag -> PageData(tag as CompoundNBT) }
                        }
                    }
                }
            }
            currentPage = currentPage ?: PageData(BookContents.MAIN_MENU_FILE)
            tabPages = tabPages ?: emptyList()
            return currentPage to tabPages
        }

        @JvmStatic
        fun openGuideBook(hand: Hand, stack: ItemStack) {
            val mc = Minecraft.getInstance()
            val equipmentSlot = if (hand == Hand.MAIN_HAND) EquipmentSlotType.MAINHAND else EquipmentSlotType.OFFHAND
            // add scheduled task to switch from network thread to main thread with OpenGL context
            mc.deferTask {
                val state = loadDataFromNBT(stack).let {
                    GuideBookState(equipmentSlot, Page(it.first), it.second.map(::Page))
                }
                mc.displayGuiScreen(GuiGuideBook(state))
            }
        }
    }

    init {
        maxStackSize = 1
    }

    class GuideBookState(val equipmentSlot: EquipmentSlotType, var currentPage: Page, bookmarks: List<Page>) {
        val bookmarks = bookmarks.toMutableList()
    }

    fun updateNBT(tag: CompoundNBT, page: IPageData, tabs: List<IPageData>) = tag.apply {
        putByte("version", 1)
        put("page", page.toTag())
        put("bookmarks", ListNBT().apply {
            tabs.map(IPageData::toTag).forEach(::add)
        })
    }

    override fun onItemRightClick(world: World, player: PlayerEntity, hand: Hand): ActionResult<ItemStack> {
        val stack = player.getHeldItem(hand)
        if (stack.item is ItemGuideBook && MainProxy.isServer(world)) {
            MainProxy.sendPacketToPlayer(
                PacketHandler.getPacket(OpenGuideBook::class.java).setHand(hand).setStack(stack),
                player,
            )
            return ActionResult(ActionResultType.SUCCESS, stack)
        }
        return ActionResult(ActionResultType.PASS, stack)
    }

    fun saveState(state: GuideBookState) {
        val stack = Minecraft.getInstance().player.getItemStackFromSlot(state.equipmentSlot)
        val compound = if (stack.hasTag()) stack.tag!! else CompoundNBT()
        // update NBT for the client
        stack.tag = updateNBT(compound, state.currentPage, state.bookmarks)

        // … and for the server
        MainProxy.sendPacketToServer(
            PacketHandler.getPacket(SetCurrentPagePacket::class.java)
                .setEquipmentSlotType(state.equipmentSlot)
                .setCurrentPage(state.currentPage)
                .setBookmarks(state.bookmarks)
        )
    }

}
