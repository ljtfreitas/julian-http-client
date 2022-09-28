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

import com.github.ljtfreitas.julian.Arguments
import com.github.ljtfreitas.julian.Endpoint
import com.github.ljtfreitas.julian.Endpoint.CallbackParameter
import com.github.ljtfreitas.julian.JavaType
import com.github.ljtfreitas.julian.Promise
import com.github.ljtfreitas.julian.Response
import com.github.ljtfreitas.julian.ResponseFn
import com.github.ljtfreitas.julian.ResponseT
import com.github.ljtfreitas.julian.Subscriber
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.lang.reflect.WildcardType
import java.util.function.Predicate.not

object KFunctionCallbackResponseT : ResponseT<Any, Unit> {

    override fun <A> bind(endpoint: Endpoint, next: ResponseFn<A, Any>): ResponseFn<A, Unit> = object : ResponseFn<A, Unit> {

        override fun join(response: Promise<out Response<A, out Throwable>>, arguments: Arguments) {
            val promise = next.run(response, arguments)

            success(endpoint.parameters(), arguments)
                .ifPresent { f -> promise.onSuccess(f::invoke) }

            failure(endpoint.parameters(), arguments)
                .ifPresent { f -> promise.onFailure(f::invoke) }

            result(endpoint.parameters(), arguments)
                .ifPresent { f -> promise.subscribe(object : Subscriber<Any, Throwable> {

                    override fun success(value: Any) {
                        f(Result.success(value))
                    }

                    override fun failure(failure: Throwable) {
                        f(Result.failure(failure))
                    }

                    override fun done() {}
                })}
        }

        @Suppress("UNCHECKED_CAST")
        private fun success(parameters: Endpoint.Parameters, arguments: Arguments) = parameters.callbacks()
            .filter(::aSuccess)
            .findFirst()
            .flatMap { arguments.of(it.position()) }
            .map { it as Function1<Any, Any> }

        @Suppress("UNCHECKED_CAST")
        private fun failure(parameters: Endpoint.Parameters, arguments: Arguments) = parameters.callbacks()
            .filter(::aFailure)
            .findFirst()
            .flatMap { arguments.of(it.position()) }
            .map { it as Function1<Throwable, Any> }

        @Suppress("UNCHECKED_CAST")
        private fun result(parameters: Endpoint.Parameters, arguments: Arguments) = parameters.callbacks()
            .filter(::aResult)
            .findFirst()
            .flatMap { arguments.of(it.position()) }
            .map { it as Function1<Result<Any>, Any> }

        override fun returnType(): JavaType = next.returnType()
    }

    override fun adapted(endpoint: Endpoint): JavaType = endpoint.parameters().callbacks()
        .filter { aSuccess(it) || aResult(it) }
        .findFirst()
        .flatMap(::argument)
        .map(JavaType::valueOf)
        .orElseGet(JavaType::none)

    private fun aSuccess(callback: CallbackParameter) = aFunction1(callback)
        && !aResult(callback)
        && argument(callback).filter(not(::aThrowable)).isPresent

    private fun aFailure(callback: CallbackParameter) = aFunction1(callback)
        && argument(callback).filter(::aThrowable).isPresent

    private fun aResult(callback: CallbackParameter) : Boolean = aFunction1(callback)
        && callback.javaType().parameterized().map { it.actualTypeArguments.first() }.filter { aResult(it) }.isPresent

    override fun test(endpoint: Endpoint) = endpoint.parameters().callbacks().anyMatch { aFunction1(it) }

    private fun aFunction1(callback: CallbackParameter) = callback.javaType().`is`(Function1::class.java)

    private fun aThrowable(type: Type) = (type == Throwable::class.java)

    private fun argument(callback: CallbackParameter) = callback.javaType().parameterized()
        .map(JavaType.Parameterized::firstArg).map { argument(it) }

    private fun argument(type: Type) : Type =
        if (type is WildcardType && type.lowerBounds.isNotEmpty())
            argument(type.lowerBounds.first())
        else if (type is ParameterizedType)
            argument(type.actualTypeArguments.first())
        else type

    private fun aResult(type: Type) : Boolean =
        if (type is WildcardType && type.lowerBounds.isNotEmpty())
            aResult(type.lowerBounds.first())
        else if (type is ParameterizedType)
            aResult(type.rawType)
        else type == Result::class.java
}
