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

package com.github.ljtfreitas.julian.k

import com.github.ljtfreitas.julian.Attempt
import com.github.ljtfreitas.julian.JavaType
import com.github.ljtfreitas.julian.Promise
import kotlin.reflect.jvm.javaType
import kotlin.reflect.typeOf

fun <T> Attempt<T>.result() : Result<T> = fold(Result.Companion::success, Result.Companion::failure)

@JvmName("promisePlus")
operator fun <A, B> Promise<A>.plus(other: Promise<B>): Promise<Pair<A, B>> = bind { a ->
    other.then { b -> a to b }
}

@JvmName("promisePairPlus")
operator fun <A, B, C> Promise<Pair<A, B>>.plus(other: Promise<C>): Promise<Triple<A, B, C>> = bind { (a, b) ->
    other.then { c -> Triple(a, b, c) }
}

@JvmName("promiseTriplePlus")
operator fun <A, B, C> Promise<Triple<A, B, C>>.plus(other: Promise<*>): Promise<Array<*>> = bind { (a, b, c) ->
    other.then { d -> arrayOf(a, b, c, d) }
}

@JvmName("promiseArrayPlus")
operator fun Promise<Array<*>>.plus(other: Promise<*>): Promise<Array<*>> = bind { a ->
    other.then { b -> arrayOf(*a, b) }
}

inline fun <reified T : Any> javaType(): JavaType = JavaType.valueOf(typeOf<T>().javaType)
