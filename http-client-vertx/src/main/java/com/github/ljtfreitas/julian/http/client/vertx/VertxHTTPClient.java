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

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;

import java.io.Closeable;
import java.io.IOException;

import com.github.ljtfreitas.julian.http.HTTPRequestDefinition;
import com.github.ljtfreitas.julian.http.client.HTTPClient;
import com.github.ljtfreitas.julian.http.client.HTTPClientRequest;

public class VertxHTTPClient implements HTTPClient, Closeable {

    private final RxVertxHTTPClient client;

    public VertxHTTPClient(Vertx vertx) {
        this.client = new RxVertxHTTPClient(rx(vertx.createHttpClient()));
    }

    public VertxHTTPClient(Vertx vertx, HttpClientOptions options) {
        this.client = new RxVertxHTTPClient(rx(vertx.createHttpClient(options)));
    }

    public VertxHTTPClient(HttpClient client) {
        this.client = new RxVertxHTTPClient(rx(client));
    }

    private io.vertx.rxjava3.core.http.HttpClient rx(HttpClient httpClient) {
        return io.vertx.rxjava3.core.http.HttpClient.newInstance(httpClient);
    }

    @Override
    public HTTPClientRequest request(HTTPRequestDefinition request) {
        return client.request(request);
    }

    @Override
    public void close() {
        client.close();
    }
}
