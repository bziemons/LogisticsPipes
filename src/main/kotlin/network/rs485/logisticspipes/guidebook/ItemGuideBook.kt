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
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory.EntityEquipmentSlot
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.nbt.NBTTagList
import net.minecraft.util.ActionResult
import net.minecraft.util.EnumActionResult
import net.minecraft.util.EnumHand
import net.minecraft.world.World
import network.rs485.logisticspipes.gui.guidebook.GuiGuideBook
import network.rs485.logisticspipes.gui.guidebook.IPageData
import network.rs485.logisticspipes.gui.guidebook.Page
import network.rs485.logisticspipes.gui.guidebook.PageData
import network.rs485.logisticspipes.network.packets.SetCurrentPagePacket

class ItemGuideBook : LogisticsItem() {

    companion object {
        /**
         * Loads the current page and the tabs from the stack's NBT. Returns Pair(currentPage, Tabs)
         */
        private fun loadDataFromNBT(stack: ItemStack): Pair<PageData, List<PageData>> {
            var currentPage: PageData? = null
            var tabPages: List<PageData>? = null
            if (stack.hasTagCompound()) {
                val nbt = stack.tagCompound!!
                if (nbt.hasKey("version")) {
                    when (nbt.getByte("version")) {
                        1.toByte() -> {
                            currentPage = PageData(nbt.getCompoundTag("page"))
                            // type 10 = NBTTagCompound, see net.minecraft.nbt.NBTBase.createNewByType
                            val tagList = nbt.getTagList("bookmarks", 10)
                            tabPages = tagList.mapNotNull { tag -> PageData(tag as NBTTagCompound) }
                        }
                    }
                }
            }
            currentPage = currentPage ?: PageData(BookContents.MAIN_MENU_FILE)
            tabPages = tabPages ?: emptyList()
            return currentPage to tabPages
        }

        @JvmStatic
        fun openGuideBook(hand: EnumHand, stack: ItemStack) {
            val mc = Minecraft.getMinecraft()
            val equipmentSlot = if (hand == EnumHand.MAIN_HAND) EntityEquipmentSlot.MAINHAND else EntityEquipmentSlot.OFFHAND
            // add scheduled task to switch from network thread to main thread with OpenGL context
            mc.addScheduledTask {
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

    class GuideBookState(val equipmentSlot: EntityEquipmentSlot, var currentPage: Page, bookmarks: List<Page>) {
        val bookmarks = bookmarks.toMutableList()
    }

    fun updateNBT(tag: NBTTagCompound, page: IPageData, tabs: List<IPageData>) = tag.apply {
        setByte("version", 1)
        setTag("page", page.toTag())
        setTag("bookmarks", NBTTagList().apply {
            tabs.map(IPageData::toTag).forEach(::appendTag)
        })
    }

    override fun onItemRightClick(world: World, player: EntityPlayer, hand: EnumHand): ActionResult<ItemStack> {
        val stack = player.getHeldItem(hand)
        if (stack.item is ItemGuideBook && MainProxy.isServer(world)) {
            MainProxy.sendPacketToPlayer(
                PacketHandler.getPacket(OpenGuideBook::class.java).setHand(hand).setStack(stack),
                player,
            )
            return ActionResult(EnumActionResult.SUCCESS, stack)
        }
        return ActionResult(EnumActionResult.PASS, stack)
    }

    fun saveState(state: GuideBookState) {
        val stack = Minecraft.getMinecraft().player.getItemStackFromSlot(state.equipmentSlot)
        val compound = if (stack.hasTagCompound()) stack.tagCompound!! else NBTTagCompound()
        // update NBT for the client
        stack.tagCompound = updateNBT(compound, state.currentPage, state.bookmarks)

        // … and for the server
        MainProxy.sendPacketToServer(
            PacketHandler.getPacket(SetCurrentPagePacket::class.java)
                .setEquipmentSlot(state.equipmentSlot)
                .setCurrentPage(state.currentPage)
                .setBookmarks(state.bookmarks)
        )
    }

}
