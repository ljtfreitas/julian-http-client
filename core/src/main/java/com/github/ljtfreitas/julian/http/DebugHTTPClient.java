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

package com.github.ljtfreitas.julian.http;

import com.github.ljtfreitas.julian.Promise;
import com.github.ljtfreitas.julian.http.client.HTTPClient;
import com.github.ljtfreitas.julian.http.client.HTTPClientRequest;
import com.github.ljtfreitas.julian.http.client.HTTPClientResponse;

import java.lang.System.Logger.Level;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.Flow.Subscription;
import java.util.stream.Collectors;

public class DebugHTTPClient implements HTTPClient {

    private static final System.Logger log = System.getLogger("DebugHTTPClient");

    private final HTTPClient source;

    public DebugHTTPClient(HTTPClient source) {
        this.source = source;
    }

    @Override
    public HTTPClientRequest request(HTTPRequestDefinition request) {
        return new DebugHTTPClientRequest(source.request(info(request)));
    }

    private class DebugHTTPClientRequest implements HTTPClientRequest  {

        private final HTTPClientRequest source;

        public DebugHTTPClientRequest(HTTPClientRequest source) {
            this.source = source;
        }

        @Override
        public Promise<HTTPClientResponse> execute() {
            return source.execute().then(DebugHTTPClient.this::info);
        }
    }

    private HTTPRequestDefinition info(HTTPRequestDefinition request) {
        request.body().ifPresentOrElse(body -> info(request, body), () -> info(request, ""));
        return request;
    }

    private void info(HTTPRequestDefinition request, HTTPRequestBody body) {
        body.serialize().subscribe(new Subscriber<>() {

            private final Collection<ByteBuffer> received = new ArrayList<>();

            @Override
            public void onSubscribe(Subscription subscription) {
                subscription.request(Integer.MAX_VALUE);
            }

            @Override
            public void onNext(ByteBuffer buffer) {
                received.add(buffer);
            }

            @Override
            public void onError(Throwable throwable) {
            }

            @Override
            public void onComplete() {
                int size = received.stream().mapToInt(Buffer::remaining).sum();

                byte[] buffer = new byte[size];
                received.forEach(b -> b.get(buffer));

                String bodyAsString = new String(buffer);

                info(request, bodyAsString);
            }
        });
    }

    private void info(HTTPRequestDefinition request, String body) {
        String headers = request.headers().all().stream().map(HTTPHeader::toString).collect(Collectors.joining("\n"));

        String message = new StringBuilder()
                .append("HTTP request ->>>>>")
                    .append("\n")
                .append(request.method()).append(" ").append(request.path())
                    .append("\n")
                .append(headers.isEmpty() ? headers : headers.concat("\n"))
                .append(body.isEmpty() ? body : body.concat("\n"))
                .append("->>>>>")
                .toString();

        log.log(Level.INFO, message);
    }

    private HTTPClientResponse info(HTTPClientResponse response) {
        String headers = response.headers().all().stream().map(HTTPHeader::toString).collect(Collectors.joining("\n"));
        String body = response.body().deserialize(String::new).map(s -> s.concat("\n")).orElse("");

        String message = new StringBuilder()
                .append("HTTP response <<<<<-")
                    .append("\n")
                .append(response.status())
                    .append("\n")
                .append(headers.isEmpty() ? headers : headers.concat("\n"))
                .append(body.isEmpty() ? body : body.concat("\n"))
                .append("<<<<<-")
                .toString();

        log.log(Level.INFO, message);

        return response;
    }
}
