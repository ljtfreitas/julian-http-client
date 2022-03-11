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

package com.github.ljtfreitas.julian.k.http.client.ktor

import com.github.ljtfreitas.julian.Promise
import com.github.ljtfreitas.julian.http.HTTPHeader
import com.github.ljtfreitas.julian.http.HTTPHeaders
import com.github.ljtfreitas.julian.http.HTTPRequestBody
import com.github.ljtfreitas.julian.http.HTTPRequestDefinition
import com.github.ljtfreitas.julian.http.HTTPResponseBody
import com.github.ljtfreitas.julian.http.HTTPStatus
import com.github.ljtfreitas.julian.http.HTTPStatusCode
import com.github.ljtfreitas.julian.http.MediaType
import com.github.ljtfreitas.julian.http.client.HTTPClient
import com.github.ljtfreitas.julian.http.client.HTTPClientRequest
import com.github.ljtfreitas.julian.http.client.HTTPClientResponse
import com.github.ljtfreitas.julian.k.http.client.ktor.KtorHTTPClient.Companion.invoke
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.call.receive
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.HttpClientEngineConfig
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.headers
import io.ktor.client.request.request
import io.ktor.client.statement.HttpResponse
import io.ktor.client.utils.EmptyContent
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.content.OutgoingContent
import io.ktor.util.toMap
import io.ktor.utils.io.ByteWriteChannel
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.future.future
import kotlinx.coroutines.jdk9.asFlow
import java.io.Closeable
import java.util.Optional

@DelicateCoroutinesApi
class KtorHTTPClient private constructor(private val client: HttpClient): HTTPClient, Closeable {

    companion object {

        operator fun invoke() = invoke(CIO)

        operator fun invoke(
            block: HttpClientConfig<*>.() -> Unit = {}
        ) = KtorHTTPClient(HttpClient() {
            block()
            expectSuccess = false
        })

        operator fun <T : HttpClientEngineConfig> invoke(
            engineFactory: HttpClientEngineFactory<T>,
            block: HttpClientConfig<T>.() -> Unit = {}
        ) = KtorHTTPClient(HttpClient(engineFactory = engineFactory) {
            block()
            expectSuccess = false
        })

        operator fun invoke(
            engine: HttpClientEngine,
            block: HttpClientConfig<*>.() -> Unit
        ) = KtorHTTPClient(HttpClient(engine = engine) {
            block()
            expectSuccess = false
        })
    }

    override fun request(request: HTTPRequestDefinition) = HTTPClientRequest {
        Promise.pending(GlobalScope.future {
            val response: HttpResponse = client.request(request.path().toURL()) {
                method = HttpMethod.parse(request.method().name)

                headers {
                    request.headers()
                        .filterNot { it.name() == HTTPHeader.CONTENT_TYPE  }
                        .forEach { appendAll(it.name(), it.values()) }
                }

                body = request.body().map { it.content(request.headers().contentType()) }.orElse(EmptyContent)
            }

            return@future response.asHTTPClientResponse()
        })
    }

    private fun HTTPHeaders.contentType() = select(HTTPHeader.CONTENT_TYPE).map { MediaType.valueOf(it.value()) }

    private fun HTTPRequestBody.content(mediaType: Optional<MediaType>): OutgoingContent = object : OutgoingContent.WriteChannelContent() {

        override suspend fun writeTo(channel: ByteWriteChannel) = serialize().asFlow().collect(channel::writeFully)

        override val contentType: ContentType? = contentType().or { mediaType }.map { ContentType.parse(it.toString()) }.orElse(null)
    }

    private suspend fun HttpResponse.asHTTPClientResponse() : HTTPClientResponse {
        val httpStatus = HTTPStatusCode.select(status.value).map { HTTPStatus(it) }
            .orElseGet { HTTPStatus(status.value, status.description) }

        val httpHeaders = headers.toMap().entries.fold(HTTPHeaders.empty()) { acc, (name, values) ->
            acc.join(HTTPHeader.create(name, values))
        }

        val bodyAsBytes: ByteArray = receive()

        return if (bodyAsBytes.isEmpty())
            HTTPClientResponse.empty(httpStatus, httpHeaders)
        else
            HTTPClientResponse.create(httpStatus, httpHeaders, HTTPResponseBody.optional(httpStatus, httpHeaders) {
                HTTPResponseBody.some(bodyAsBytes)
            })
    }

    override fun close() = client.close()
}