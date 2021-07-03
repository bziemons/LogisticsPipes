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

package network.rs485.logisticspipes.gui

import com.mojang.blaze3d.platform.GlStateManager
import logisticspipes.utils.gui.LogisticsBaseGuiScreen
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Gui
import net.minecraft.client.renderer.GlStateManager
import network.rs485.logisticspipes.util.TextUtil
import net.minecraft.client.gui.widget.Widget
import net.minecraftforge.fml.client.config.GuiUtils
import network.rs485.logisticspipes.util.math.Rectangle
import network.rs485.logisticspipes.util.opaque

open class Label(fullText: String, internal val x: Int, internal val y: Int, internal val maxLength: Int, internal val textColor: Int, internal val backgroundColor: Int) : Widget(x, y, fullText) {

    open val overflows: Boolean get() = fullRect.width > maxLength

    internal val fontRenderer = Minecraft.getInstance().fontRenderer

    internal val fullRect = Rectangle().setPos(x, y)
    internal val trimmedRect = Rectangle().setPos(x, y)

    internal var trimmedText: String = ""

    init {
        textUpdatedInternal()
    }

    open fun draw(mouseX: Int, mouseY: Int) {
        isHovered = isHovered()
        GlStateManager.pushMatrix()
        GlStateManager.translatef(fullRect.x0, fullRect.y0, 0.0f)
        if (overflows && isHovered) {
            GuiUtils.drawGradientRect(blitOffset, 0, -1, fullRect.roundedWidth, fullRect.roundedHeight + 1, backgroundColor, backgroundColor)
            // Outlines
            LogisticsBaseGuiScreen.drawHorizontalGradientRect(fullRect.roundedWidth, -2, fullRect.roundedWidth + 1, fullRect.roundedHeight + 1, 0, textColor.opaque(), textColor.opaque())
            LogisticsBaseGuiScreen.drawHorizontalGradientRect(0, -2, fullRect.roundedWidth, -1, 0, 0x0, textColor.opaque())
            LogisticsBaseGuiScreen.drawHorizontalGradientRect(0, fullRect.roundedHeight, fullRect.roundedWidth, fullRect.roundedHeight + 1, 0, 0x0, textColor.opaque())
            message
        } else {
            trimmedText
        }.also {
            fontRenderer.drawString(it, 0f, 0f, textColor)
        }
        GlStateManager.translatef(-fullRect.x0, -fullRect.y0, 0.0f)
        GlStateManager.popMatrix()
    }

    fun setText(newFullText: String) {
        message = newFullText
        textUpdated()
    }

    protected open fun textUpdated() {
        textUpdatedInternal()
    }

    private fun textUpdatedInternal() {
        fullRect.setSize(fontRenderer.getStringWidth(message), fontRenderer.FONT_HEIGHT)

        trimmedText = TextUtil.getTrimmedString(message, maxLength, fontRenderer)
        trimmedRect.setSize(fontRenderer.getStringWidth(trimmedText), fontRenderer.FONT_HEIGHT)

        val offset = (maxLength - trimmedRect.roundedWidth) / 2
        fullRect.setPos(x + offset, y)
        trimmedRect.setPos(x + offset, y)
    }

    fun isTextEqual(text: String): Boolean = message === text

}