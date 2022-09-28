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
import com.github.ljtfreitas.julian.Kind
import com.github.ljtfreitas.julian.Promise
import com.github.ljtfreitas.julian.Response
import com.github.ljtfreitas.julian.ResponseFn
import com.github.ljtfreitas.julian.ResponseT
import com.github.ljtfreitas.julian.http.RecoverableHTTPResponse
import java.lang.reflect.Type
import java.lang.reflect.WildcardType
import java.util.Optional
import java.util.function.Supplier

object RecoverableEitherResponseT : ResponseT<Any, Either<Any, Any>> {

    override fun test(endpoint: Endpoint) = endpoint.returnType().let { returnType ->
        returnType.`is`(Either::class.java)
            && returnType.parameterized()
                    .map(JavaType.Parameterized::firstArg)
                    .map(::argument)
                    .map(JavaType::valueOf)
                    .filter { left -> !left.compatible(Throwable::class.java) }
                    .isPresent
    }

    override fun adapted(endpoint: Endpoint): JavaType = endpoint.returnType().parameterized()
        .map { it.actualTypeArguments[1] }
        .map(::argument)
        .orElseGet { Any::class.java }
        .let(JavaType::valueOf)

    private fun argument(type: Type) : Type =
        if (type is WildcardType && type.upperBounds.isNotEmpty())
            argument(type.upperBounds.first())
        else type

    override fun <A> bind(endpoint: Endpoint, next: ResponseFn<A, Any>) = object : ResponseFn<A, Either<Any, Any>> {

        override fun run(response: Promise<out Response<A, out Throwable>>, arguments: Arguments): Promise<Either<Any, Any>> {
            val leftClassType: Class<out Any> = JavaType.valueOf(endpoint.returnType().parameterized()
                .map(JavaType.Parameterized::firstArg)
                .orElse(Any::class.java))
                .rawClassType()

            val left: Promise<Optional<Promise<Either<Any, Any>>>> = response.then { r ->
                r.cast(object : Kind<RecoverableHTTPResponse<A>>() {})
                    .map { recoverable -> recoverable.recover(leftClassType) }
                    .map { recovered -> Promise.done(recovered) }
                    .map { recovered -> recovered.bind { rec -> rec.fold({ success -> Promise.done(success) }, { failure -> Promise.failed(failure)})} }
                    .map { recovered -> recovered.then { it.left() } }
            }

            val right = Supplier<Promise<Either<Any, Any>>> { next.run(response, arguments).then { it.right() } }

            return left.bind { recovered -> recovered.orElseGet(right) }
        }

        override fun returnType() = next.returnType()
    }
}

class RecoverableEitherResponseTProxy : ResponseT<Any, Either<Any, Any>> by RecoverableEitherResponseT