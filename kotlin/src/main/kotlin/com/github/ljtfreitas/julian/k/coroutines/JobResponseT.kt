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

import com.github.ljtfreitas.julian.Arguments
import com.github.ljtfreitas.julian.Endpoint
import com.github.ljtfreitas.julian.JavaType
import com.github.ljtfreitas.julian.Promise
import com.github.ljtfreitas.julian.Response
import com.github.ljtfreitas.julian.ResponseFn
import com.github.ljtfreitas.julian.ResponseT
import kotlinx.coroutines.Job

object JobResponseT : ResponseT<Unit, Job> {

    override fun <A> bind(endpoint: Endpoint, next: ResponseFn<A, Unit>): ResponseFn<A, Job> = object : ResponseFn<A, Job> {

        override fun join(response: Promise<out Response<A, out Throwable>>, arguments: Arguments) = next.run(response, arguments).job()

        override fun returnType(): JavaType = next.returnType()
    }

    override fun adapted(endpoint: Endpoint): JavaType = JavaType.none()

    override fun test(endpoint: Endpoint) = endpoint.returnType().`is`(Job::class.java)
}
