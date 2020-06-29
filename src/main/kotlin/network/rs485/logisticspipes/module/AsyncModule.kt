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

package network.rs485.logisticspipes.module

import kotlinx.coroutines.*
import kotlinx.coroutines.time.withTimeout
import logisticspipes.LogisticsPipes
import logisticspipes.modules.AbstractModule
import java.time.Duration

abstract class AsyncModule<S, C> : AbstractModule() {
    protected open val everyNthTick: Int = 20
    private var currentTick: Int = 0
    private var currentTask: Deferred<C?>? = null
    private var currentSyncWork: Runnable? = null
    private val lock: Any = object {}

    @ExperimentalCoroutinesApi
    override fun tick() {
        when {
            currentTask?.isActive == true -> runSyncWork()
            currentTask?.isCompleted == true -> try {
                runSyncWork()
                completeTick(currentTask!!)
            } finally {
                currentTask = null
            }
            else -> if (pipe.isNthTick(everyNthTick)) {
                val setup = tickSetup()
                currentTask = GlobalScope.async {
                    try {
                        return@async withTimeout(Duration.ofSeconds(10)) {
                            tickAsync(setup)
                        }
                    } catch (e: TimeoutCancellationException) {
                        LogisticsPipes.log.warn("Timeout on async module $this")
                    } catch (e: RuntimeException) {
                        LogisticsPipes.log.error("Error on async module $this", e)
                    }
                    return@async null
                }
            }
        }
    }

    private fun runSyncWork() {
        if (currentSyncWork != null) {
            synchronized(lock) { currentSyncWork?.also { currentSyncWork = null } }?.run()
        }
    }

    fun appendSyncWork(runnable: Runnable) {
        synchronized(lock) {
            currentSyncWork = currentSyncWork?.let { previousSyncWork ->
                Runnable {
                    previousSyncWork.run()
                    runnable.run()
                }
            } ?: runnable
        }
    }

    abstract fun tickSetup(): S

    abstract fun completeTick(task: Deferred<C?>)

    abstract suspend fun tickAsync(setupObject: S): C
}