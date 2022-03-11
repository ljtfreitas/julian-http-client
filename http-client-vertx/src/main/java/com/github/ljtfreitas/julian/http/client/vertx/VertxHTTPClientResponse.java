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

package com.github.ljtfreitas.julian.http.client.vertx;

import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import io.vertx.rxjava3.core.buffer.Buffer;
import io.vertx.rxjava3.core.http.HttpClientResponse;

import java.util.Optional;
import java.util.function.Function;

import com.github.ljtfreitas.julian.Response;
import com.github.ljtfreitas.julian.http.HTTPHeader;
import com.github.ljtfreitas.julian.http.HTTPHeaders;
import com.github.ljtfreitas.julian.http.HTTPResponseBody;
import com.github.ljtfreitas.julian.http.HTTPStatus;
import com.github.ljtfreitas.julian.http.HTTPStatusCode;
import com.github.ljtfreitas.julian.http.client.HTTPClientResponse;

class VertxHTTPClientResponse implements HTTPClientResponse {

    private final HTTPClientResponse response;

    private VertxHTTPClientResponse(HTTPClientResponse response) {
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

    static Single<VertxHTTPClientResponse> valueOf(HttpClientResponse response) {
        HTTPStatus status = HTTPStatusCode.select(response.statusCode()).map(HTTPStatus::new)
                .orElseGet(() -> new HTTPStatus(response.statusCode(), response.statusMessage()));

        HTTPHeaders headers = response.headers().names().stream()
                .map(name -> HTTPHeader.create(name, response.headers().getAll(name)))
                .reduce(HTTPHeaders.empty(), HTTPHeaders::join, (a, b) -> b);

        Maybe<byte[]> bodyAsBytes = response.body().map(Buffer::getBytes).toMaybe();

        return bodyAsBytes
                .map(body -> HTTPResponseBody.optional(status, headers, () -> HTTPResponseBody.some(body)))
                .map(body -> new VertxHTTPClientResponse(HTTPClientResponse.create(status, headers, body)))
                .switchIfEmpty(Single.fromCallable(() -> new VertxHTTPClientResponse(HTTPClientResponse.empty(status, headers))));
    }
}
