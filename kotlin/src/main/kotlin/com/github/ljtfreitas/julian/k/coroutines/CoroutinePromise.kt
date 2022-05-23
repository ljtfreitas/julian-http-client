/*
 * Copyright (C) 2021 Tiago de Freitas Lima
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.github.ljtfreitas.julian.k.coroutines

import com.github.ljtfreitas.julian.Attempt
import com.github.ljtfreitas.julian.Promise
import com.github.ljtfreitas.julian.Subscriber
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.future.await
import kotlinx.coroutines.future.future
import kotlinx.coroutines.launch
import java.util.concurrent.CompletableFuture
import java.util.function.BiFunction
import java.util.function.Consumer
import java.util.function.Function
import java.util.function.Predicate

@OptIn(DelicateCoroutinesApi::class)
class CoroutinePromise<T>(private val future: CompletableFuture<T>) : Promise<T> {

	override fun onSuccess(fn: Consumer<in T>): Promise<T> = apply {
		GlobalScope.launch {
			fn.accept(future.await())
		}
	}

	override fun <R : Any?> then(fn: Function<in T, R>): Promise<R> = CoroutinePromise(
		GlobalScope.future {
			fn.apply(future.await())
		}
	)

	override fun <R : Any?> bind(fn: Function<in T, Promise<R>>): Promise<R> {
		TODO("Not yet implemented")
	}

	override fun <T2 : Any?, R : Any?> zip(other: Promise<T2>?, fn: BiFunction<in T, in T2, R>?): Promise<R> {
		TODO("Not yet implemented")
	}

	override fun join(): Attempt<T> {
		TODO("Not yet implemented")
	}

	override fun onFailure(fn: Consumer<in Exception>?): Promise<T> {
		TODO("Not yet implemented")
	}

	override fun <Err : Exception?> failure(fn: Function<in Exception, Err>?): Promise<T> {
		TODO("Not yet implemented")
	}

	override fun future(): CompletableFuture<T> {
		TODO("Not yet implemented")
	}

	override fun subscribe(subscriber: Subscriber<in T>?): Promise<T> {
		TODO("Not yet implemented")
	}

	override fun <R : Any?> fold(success: Function<in T, R>?, failure: Function<in Exception, R>?): R {
		TODO("Not yet implemented")
	}

	override fun <Err : Exception?> recover(expected: Class<out Err>?, fn: Function<in Err, T>?): Promise<T> {
		TODO("Not yet implemented")
	}

	override fun recover(p: Predicate<in Exception>?, fn: Function<in Exception, T>?): Promise<T> {
		TODO("Not yet implemented")
	}

	override fun recover(fn: Function<in Exception, T>?): Promise<T> {
		TODO("Not yet implemented")
	}
}