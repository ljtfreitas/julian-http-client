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

package com.github.ljtfreitas.julian.k.arrow

import arrow.core.NonEmptyList
import com.github.ljtfreitas.julian.Arguments
import com.github.ljtfreitas.julian.Endpoint
import com.github.ljtfreitas.julian.JavaType
import com.github.ljtfreitas.julian.Promise
import com.github.ljtfreitas.julian.Response
import com.github.ljtfreitas.julian.ResponseFn
import com.github.ljtfreitas.julian.ResponseT
import java.lang.reflect.Type
import java.lang.reflect.WildcardType

object NonEmptyListResponseT : ResponseT<Collection<Any>, NonEmptyList<Any>> {

    override fun test(endpoint: Endpoint) = endpoint.returnType().`is`(NonEmptyList::class.java)

    override fun adapted(endpoint: Endpoint): JavaType = endpoint.returnType().parameterized()
        .map(JavaType.Parameterized::firstArg)
        .map(::argument)
        .orElseGet { Any::class.java }
        .let { JavaType.parameterized(Collection::class.java, it) }

    private fun argument(type: Type) : Type =
        if (type is WildcardType && type.upperBounds.isNotEmpty())
            argument(type.upperBounds.first())
        else type

    override fun <A> bind(endpoint: Endpoint, next: ResponseFn<A, Collection<Any>>) = object : ResponseFn<A, NonEmptyList<Any>> {

        override fun run(response: Promise<out Response<A?, out Throwable>>, arguments: Arguments): Promise<NonEmptyList<Any>> =
            next.run(response, arguments).then { NonEmptyList.fromListUnsafe(it.toList()) }

        override fun returnType(): JavaType = next.returnType()
    }
}

class NonEmptyListResponseTProxy : ResponseT<Collection<Any>, NonEmptyList<Any>> by NonEmptyListResponseT