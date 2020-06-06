/*
 * Copyright (c) 2016  RS485
 *
 * "LogisticsPipes" is distributed under the terms of the Minecraft Mod Public
 * License 1.0.1, or MMPL. Please check the contents of the license located in
 * https://github.com/RS485/LogisticsPipes/blob/dev/LICENSE.md
 *
 * This file can instead be distributed under the license terms of the
 * MIT license:
 *
 * Copyright (c) 2016  RS485
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

package network.rs485.grow;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import logisticspipes.LogisticsPipes;

/**
 * Getting Rid Of Work.
 * Singleton, use getInstance()
 */
@SuppressWarnings("WeakerAccess")
public final class GROW {

	private static final GROW instance = new GROW();
	private ThreadPoolExecutor backgroundPool;

	public static void UP() {}

	private GROW() {
		backgroundPool = (ThreadPoolExecutor) Executors.newFixedThreadPool(2);
	}

	public static GROW getInstance() {
		return instance;
	}

	@Nonnull
	public static <T> T asyncWorkAround(@Nonnull CompletableFuture<T> future) {
		if (!future.isDone()) {
			throw new RuntimeException("Async not ready");
		}
		try {
			return future.get();
		} catch (InterruptedException | ExecutionException e) {
			throw new RuntimeException("Async exception", e);
		}
	}

	@Nullable
	public static <T> T unwrapUnknownFuture(@Nonnull CompletableFuture completableFuture) {
		try {
			//noinspection unchecked
			return (T) completableFuture.get();
		} catch (ClassCastException | InterruptedException | ExecutionException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Nullable
	public static <T> T unwrapFuture(@Nonnull CompletableFuture<T> completableFuture) {
		try {
			return completableFuture.get();
		} catch (ClassCastException | InterruptedException | ExecutionException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Nonnull
	public CompletableFuture<Void> submit(Priority priority, Runnable runnable) {
		if (priority == Priority.HIGH) {
			if (backgroundPool.getActiveCount() < backgroundPool.getLargestPoolSize()) {
				return CompletableFuture.runAsync(runnable, backgroundPool);
			} else {
				// run in Java's common pool
				return CompletableFuture.runAsync(runnable);
			}
		} else {
			return CompletableFuture.runAsync(runnable, backgroundPool);
		}
	}

	public static void asyncComplete(Object result, Throwable error, String func, Object object) {
		if (error == null) {
			LogisticsPipes.log.debug("Ran " + func + " on " + Objects.toString(object) + " with result " + Objects.toString(result));
		} else {
			System.err.println("Error when running " + func + " on " + Objects.toString(object) + ":");
			error.printStackTrace();
		}
	}

	public enum Priority {
		BACKGROUND,
		HIGH;

		public int getRating() {
			return this.ordinal();
		}
	}
}
