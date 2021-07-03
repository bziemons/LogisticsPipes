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

package network.rs485.logisticspipes.property

import logisticspipes.LPItems
import logisticspipes.items.ItemModule
import logisticspipes.modules.LogisticsModule
import net.minecraft.item.Item
import net.minecraft.nbt.CompoundNBT
import net.minecraft.util.registry.Registry
import net.minecraft.util.registry.Registry.REGISTRY

const val SLOT_INDEX_KEY = "slotted_module.slot"
const val MODULE_NAME_KEY = "slotted_module.name"

class SlottedModuleListProperty(slots: Int, override val tagKey: String) :
    ListProperty<SlottedModule>(MutableList(slots) { SlottedModule(it, null) }) {

    override fun defaultValue(idx: Int): SlottedModule = SlottedModule(idx, null)

    override fun readSingleFromNBT(tag: CompoundNBT, key: String): SlottedModule {
        val slottedModuleTag = tag.getCompound(key)
        val slot = slottedModuleTag.getInt(SLOT_INDEX_KEY)
        return list.getOrElse(slot) {
            val moduleName = if (slottedModuleTag.contains(MODULE_NAME_KEY)) {
                slottedModuleTag.getString(MODULE_NAME_KEY)
            } else null
            val moduleResource = moduleName?.let { LPItems.modules[it] }
            val module = moduleResource?.let {
                Registry.ITEM.getValue(moduleResource).takeIf { it.isPresent }?.get() as? ItemModule
            }
            SlottedModule(slot = slot, module = module?.getModule(null, null, null))
        }.also { it.module?.readFromNBT(slottedModuleTag) }
    }

    override fun writeSingleToNBT(tag: CompoundNBT, key: String, value: SlottedModule) {
        tag.put(key, CompoundNBT()
            .also { value.module?.writeToNBT(it) }
            .apply {
                putInt(SLOT_INDEX_KEY, value.slot)
                value.module?.also { putString(MODULE_NAME_KEY, it.lpName) }
            })
    }

    override fun copyValue(obj: SlottedModule): SlottedModule = obj.copy(slot = obj.slot, module = null)

    override fun copyProperty(): SlottedModuleListProperty =
        SlottedModuleListProperty(size, tagKey).apply { addAll(list) }

    fun set(slot: Int, module: LogisticsModule) = set(slot, SlottedModule(slot, module))
    fun clear(slot: Int) = set(slot, SlottedModule(slot, null))

}

data class SlottedModule(val slot: Int, val module: LogisticsModule?) {
    fun isEmpty() = module == null
}
