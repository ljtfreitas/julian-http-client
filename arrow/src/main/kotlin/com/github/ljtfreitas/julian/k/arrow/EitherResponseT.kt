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

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.github.ljtfreitas.julian.Arguments
import com.github.ljtfreitas.julian.Endpoint
import com.github.ljtfreitas.julian.JavaType
import com.github.ljtfreitas.julian.Promise
import com.github.ljtfreitas.julian.Response
import com.github.ljtfreitas.julian.ResponseFn
import com.github.ljtfreitas.julian.ResponseT

object EitherResponseT : ResponseT<Any, Either<Exception, Any>> {

    override fun test(endpoint: Endpoint) = endpoint.returnType().let { returnType -> returnType.`is`(Either::class.java)
            && returnType.parameterized()
                    .map(JavaType.Parameterized::firstArg)
                    .map(JavaType::valueOf)
                    .filter { left -> left.compatible(java.lang.Exception::class.java) }
                    .isPresent
    }

    override fun adapted(endpoint: Endpoint): JavaType = endpoint.returnType().parameterized()
        .map { it.actualTypeArguments[1] }
        .orElseGet { Any::class.java }
        .let(JavaType::valueOf)

    override fun <A> bind(endpoint: Endpoint, next: ResponseFn<A, Any>) = object : ResponseFn<A, Either<Exception, Any>> {

        @Suppress("UNCHECKED_CAST")
        override fun run(response: Promise<out Response<A>>, arguments: Arguments): Promise<Either<Exception, Any>> {
            val leftClassType: Class<out Any> = JavaType.valueOf(endpoint.returnType().parameterized()
                .map(JavaType.Parameterized::firstArg)
                .orElse(Exception::class.java))
                .rawClassType()

            return next.run(response, arguments)
                .then<Either<Exception, Any>> { it.right() }
                .recover(leftClassType::isInstance) { it.left() }
        }

        override fun returnType() = next.returnType()
    }
}