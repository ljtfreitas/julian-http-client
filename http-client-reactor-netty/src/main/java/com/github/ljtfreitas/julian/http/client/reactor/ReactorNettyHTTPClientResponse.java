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

package com.github.ljtfreitas.julian.http.client.reactor;

import com.github.ljtfreitas.julian.http.OptionalHTTPResponseBody;
import reactor.core.publisher.Mono;
import reactor.netty.ByteBufMono;
import reactor.netty.http.client.HttpClientResponse;

import java.util.Optional;
import java.util.function.Function;

import com.github.ljtfreitas.julian.Response;
import com.github.ljtfreitas.julian.http.DefaultHTTPResponseBody;
import com.github.ljtfreitas.julian.http.HTTPHeader;
import com.github.ljtfreitas.julian.http.HTTPHeaders;
import com.github.ljtfreitas.julian.http.HTTPResponseBody;
import com.github.ljtfreitas.julian.http.HTTPStatus;
import com.github.ljtfreitas.julian.http.HTTPStatusCode;
import com.github.ljtfreitas.julian.http.client.HTTPClientResponse;

class ReactorNettyHTTPClientResponse implements HTTPClientResponse {

    private final HTTPClientResponse response;

    private ReactorNettyHTTPClientResponse(HTTPClientResponse response) {
        this.response = response;
    }

    @Override
    public HTTPStatus status() {
        return response.status();
    }

    @Override
    public HTTPHeaders headers() {
        return response.headers();
    }

    @Override
    public HTTPResponseBody body() {
        return response.body();
    }

    @Override
    public <T, R extends Response<T>> Optional<R> failure(Function<? super HTTPClientResponse, R> fn) {
        return response.failure(fn);
    }

    @Override
    public <T, R extends Response<T>> Optional<R> success(Function<? super HTTPClientResponse, R> fn) {
        return response.success(fn);
    }

    static Mono<ReactorNettyHTTPClientResponse> valueOf(HttpClientResponse response, ByteBufMono bodyAsBuffer) {
        HTTPStatus status = HTTPStatusCode.select(response.status().code()).map(HTTPStatus::new)
                .orElseGet(() -> new HTTPStatus(response.status().code(), response.status().reasonPhrase()));

        HTTPHeaders headers = response.responseHeaders().names().stream()
                .map(name -> HTTPHeader.create(name, response.responseHeaders().getAll(name)))
                .reduce(HTTPHeaders.empty(), HTTPHeaders::join, (a, b) -> b);

        return bodyAsBuffer.asByteArray()
                .map(bodyAsByteArray -> new OptionalHTTPResponseBody(status, headers, () -> new DefaultHTTPResponseBody(bodyAsByteArray)))
                .map(body -> new ReactorNettyHTTPClientResponse(HTTPClientResponse.create(status, headers, body)))
                .switchIfEmpty(Mono.fromCallable(() -> new ReactorNettyHTTPClientResponse(HTTPClientResponse.empty(status, headers))));
    }
}
