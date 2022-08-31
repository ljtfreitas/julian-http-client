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

package com.github.ljtfreitas.julian.k.http.arrow

import arrow.fx.coroutines.CircuitBreaker
import com.github.ljtfreitas.julian.Promise
import com.github.ljtfreitas.julian.http.FailureHTTPResponse
import com.github.ljtfreitas.julian.http.HTTPHeaders
import com.github.ljtfreitas.julian.http.HTTPMethod
import com.github.ljtfreitas.julian.http.HTTPRequest
import com.github.ljtfreitas.julian.http.HTTPRequestBody
import com.github.ljtfreitas.julian.http.HTTPRequestInterceptor
import com.github.ljtfreitas.julian.http.HTTPResponse
import com.github.ljtfreitas.julian.k.coroutines.await
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.future.future
import java.net.URI
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

class CircuitBreakerHTTPRequestInterceptor(private val circuitBreaker: CircuitBreaker,
                                           private val predicate: (HTTPResponse<*>) -> Boolean = { it.status().isSuccess},
                                           private val coroutineContext: CoroutineContext = EmptyCoroutineContext) : HTTPRequestInterceptor {

    override fun <T : Any> intercepts(request: Promise<HTTPRequest<T>>): Promise<HTTPRequest<T>> = request.then {
        CircuitBreakerHTTPRequest(circuitBreaker, predicate, coroutineContext, it)
    }
}

@OptIn(DelicateCoroutinesApi::class)
class CircuitBreakerHTTPRequest<T>(
    private val circuitBreaker: CircuitBreaker,
    private val predicate: (HTTPResponse<*>) -> Boolean,
    private val coroutineContext: CoroutineContext,
    private val request: HTTPRequest<T>
) : HTTPRequest<T> by request {

    override fun path(path: URI): HTTPRequest<T> =
        CircuitBreakerHTTPRequest(circuitBreaker, predicate, coroutineContext, request.path(path))

    override fun method(method: HTTPMethod): HTTPRequest<T> =
        CircuitBreakerHTTPRequest(circuitBreaker, predicate, coroutineContext, request.method(method))

    override fun headers(headers: HTTPHeaders): HTTPRequest<T> =
        CircuitBreakerHTTPRequest(circuitBreaker, predicate, coroutineContext, request.headers(headers))

    override fun body(body: HTTPRequestBody): HTTPRequest<T> =
        CircuitBreakerHTTPRequest(circuitBreaker, predicate, coroutineContext, request.body(body))

    override fun execute(): Promise<HTTPResponse<T>> = Promise.pending(
        GlobalScope.future {
            circuitBreaker.protectOrThrow {
                val response = request.execute().await()

                if (predicate(response)) {
                    response
                } else {
                    throw when (response) {
                        is FailureHTTPResponse -> response.asException()
                        else -> CircuitBreaker.ExecutionRejected(reason = "rejected: $response", state = circuitBreaker.state())
                    }
                }
            }
        }
    )
}
