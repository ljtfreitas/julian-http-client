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

import io.reactivex.rxjava3.core.Flowable;
import io.vertx.core.http.RequestOptions;
import io.vertx.rxjava3.core.buffer.Buffer;
import io.vertx.rxjava3.core.http.HttpClient;

import com.github.ljtfreitas.julian.Promise;
import com.github.ljtfreitas.julian.http.client.HTTPClientRequest;
import com.github.ljtfreitas.julian.http.client.HTTPClientResponse;
import com.github.ljtfreitas.julian.rxjava3.SinglePromise;

class VertxHTTPClientRequest implements HTTPClientRequest {

    private final HttpClient client;
    private final RequestOptions options;
    private final Flowable<Buffer> bodyAsFlowable;

    VertxHTTPClientRequest(HttpClient client, RequestOptions options, Flowable<Buffer> bodyAsFlowable) {
        this.client = client;
        this.options = options;
        this.bodyAsFlowable = bodyAsFlowable;
    }

    @Override
    public Promise<HTTPClientResponse> execute() {
        return new SinglePromise<>(client.rxRequest(options)
                .flatMap(r -> r.rxSend(bodyAsFlowable))
                .flatMap(VertxHTTPClientResponse::valueOf)
                .cast(HTTPClientResponse.class));
    }
}
