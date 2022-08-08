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

package com.github.ljtfreitas.julian.http.client.okhttp;

import com.github.ljtfreitas.julian.Attempt;
import com.github.ljtfreitas.julian.http.HTTPRequestBody;
import com.github.ljtfreitas.julian.http.HTTPRequestDefinition;
import com.github.ljtfreitas.julian.http.client.HTTPClient;
import com.github.ljtfreitas.julian.http.client.HTTPClientRequest;
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

import java.io.Closeable;
import java.util.Optional;

class OkHTTPClient implements HTTPClient, Closeable {

    private final OkHttpClient client;

    public OkHTTPClient() {
        this(new OkHttpClient.Builder().build());
    }

    public OkHTTPClient(OkHttpClient okHttpClient) {
        this.client = okHttpClient;
    }

    @Override
    public HTTPClientRequest request(HTTPRequestDefinition request) {
        RequestBody body = request.body()
                .flatMap(b -> b.contentType()
                        .map(c -> MediaType.parse(c.toString()))
                        .map(mediaType -> new OkHTTPRequestBody(mediaType, b)))
                .orElse(null);

        Request okHttpRequest = new Request.Builder()
                .url(Attempt.run(request.path()::toURL).prop())
                .headers(request.headers().all().stream()
                        .reduce(new Headers.Builder(),
                                (b, h) -> b.set(h.name(), h.valuesAsString()),
                                (a, b) -> b)
                        .build())
                .method(request.method().name(), body)
                .build();

        return new OkHTTPClientRequest(client, okHttpRequest);
    }

    @Override
    public void close() {
        Optional.ofNullable(client.cache()).ifPresent(c -> Attempt.just(c::close));
        Attempt.just(client.dispatcher().executorService()::shutdown);
    }
}
