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

import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.HttpMethod;
import reactor.adapter.JdkFlowAdapter;
import reactor.core.publisher.Flux;
import reactor.netty.ByteBufFlux;
import reactor.netty.http.client.HttpClient;
import reactor.netty.http.client.HttpClient.ResponseReceiver;

import com.github.ljtfreitas.julian.http.HTTPRequestBody;
import com.github.ljtfreitas.julian.http.HTTPRequestDefinition;
import com.github.ljtfreitas.julian.http.client.HTTPClient;
import com.github.ljtfreitas.julian.http.client.HTTPClientRequest;

public class ReactorNettyHTTPClient implements HTTPClient {

    private final HttpClient client;

    public ReactorNettyHTTPClient() {
        this(HttpClient.create());
    }

    public ReactorNettyHTTPClient(HttpClient client) {
        this.client = client;
    }

    @Override
    public HTTPClientRequest request(HTTPRequestDefinition request) {
        ResponseReceiver<?> receiver = client
                .headers(headers -> request.headers().forEach(h -> headers.add(h.name(), h.values())))
                .request(HttpMethod.valueOf(request.method().name()))
                .uri(request.path())
                .send(ByteBufFlux.fromInbound(request.body()
                        .map(HTTPRequestBody::serialize)
                        .map(JdkFlowAdapter::flowPublisherToFlux)
                        .map(f -> f.map(Unpooled::wrappedBuffer))
                        .orElseGet(Flux::empty)));

        return new ReactorNettyHTTPClientRequest(receiver);
    }
}
