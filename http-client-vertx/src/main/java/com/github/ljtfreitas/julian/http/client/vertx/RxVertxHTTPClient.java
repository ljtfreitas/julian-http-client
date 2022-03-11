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
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.RequestOptions;
import io.vertx.rxjava3.core.buffer.Buffer;
import io.vertx.rxjava3.core.http.HttpClient;

import java.io.Closeable;
import java.io.IOException;

import org.reactivestreams.FlowAdapters;

import com.github.ljtfreitas.julian.http.HTTPRequestDefinition;
import com.github.ljtfreitas.julian.http.client.HTTPClient;
import com.github.ljtfreitas.julian.http.client.HTTPClientRequest;

class RxVertxHTTPClient implements HTTPClient, Closeable {

    private final HttpClient client;

    RxVertxHTTPClient(HttpClient client) {
        this.client = client;
    }

    @Override
    public HTTPClientRequest request(HTTPRequestDefinition request) {
        RequestOptions options = new RequestOptions()
                .setAbsoluteURI(request.path().toString())
                .setMethod(HttpMethod.valueOf(request.method().name()))
                .setHeaders(request.headers().all().stream().reduce(MultiMap.caseInsensitiveMultiMap(),
                        (m, h) -> m.add(h.name(), h.values()), (a, b) -> b));

        Flowable<Buffer> bodyAsFlowable = request.body()
                .map(b -> FlowAdapters.toPublisher(b.serialize()))
                .map(Flowable::fromPublisher)
                .orElseGet(Flowable::empty)
                .map(buf -> Buffer.buffer(buf.array()));

        return new VertxHTTPClientRequest(client, options, bodyAsFlowable);
    }

    @Override
    public void close() {
        client.close().blockingAwait();
    }
}
