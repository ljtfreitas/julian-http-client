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

import arrow.core.Eval
import com.github.ljtfreitas.julian.Arguments
import com.github.ljtfreitas.julian.Endpoint
import com.github.ljtfreitas.julian.JavaType
import com.github.ljtfreitas.julian.Promise
import com.github.ljtfreitas.julian.Response
import com.github.ljtfreitas.julian.ResponseFn
import com.github.ljtfreitas.julian.ResponseT

object EvalResponseT : ResponseT<Any, Eval<Any>> {

    override fun test(endpoint: Endpoint) = endpoint.returnType().`is`(Eval::class.java)

    override fun adapted(endpoint: Endpoint): JavaType = endpoint.returnType().parameterized()
        .map(JavaType.Parameterized::firstArg)
        .orElseGet { Any::class.java }
        .let(JavaType::valueOf)

    override fun <A> bind(endpoint: Endpoint, next: ResponseFn<A, Any>) = object : ResponseFn<A, Eval<Any>> {

        override fun join(response: Promise<out Response<A>>, arguments: Arguments): Eval<Any> = Eval.later {
            next.join(response, arguments)
        }

        override fun returnType(): JavaType = next.returnType()
    }
}

class EvalResponseTProxy : ResponseT<Any, Eval<Any>> by EvalResponseT