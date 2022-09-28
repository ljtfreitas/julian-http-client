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
@file:JvmName("Coroutines")

package com.github.ljtfreitas.julian.k.coroutines

import com.github.ljtfreitas.julian.Attempt
import com.github.ljtfreitas.julian.Promise
import com.github.ljtfreitas.julian.k.result
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.future.asDeferred
import kotlinx.coroutines.future.await
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

fun <T> Promise<T>.deferred(): Deferred<T> = future().asDeferred()

fun <T> Promise<T>.job(): Job = future().asDeferred()

suspend fun <T> Promise<T>.await(): T = future().await()

suspend fun <T> Promise<T>.result() : Result<T> = runCatching { await() }

suspend fun <T> Promise<T>.attempt() : Attempt<T> = runCatching { await() }.fold({ Attempt.success(it) }, { Attempt.failed(it) })

suspend fun <T> promise(context: CoroutineContext = EmptyCoroutineContext, block: () -> T): Promise<T> = coroutineScope {
    try {
        withContext(context = context) {
            block()

        }.let {
            Promise.done(it)
        }

    } catch (e: Exception) {
        Promise.failed(e)
    }
}


