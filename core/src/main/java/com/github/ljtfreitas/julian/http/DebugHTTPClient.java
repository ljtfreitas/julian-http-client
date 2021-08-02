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

import java.lang.System.Logger.Level;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.Flow.Subscription;
import java.util.stream.Collectors;

import com.github.ljtfreitas.julian.Except;
import com.github.ljtfreitas.julian.Promise;
import com.github.ljtfreitas.julian.http.client.HTTPClient;
import com.github.ljtfreitas.julian.http.client.HTTPClientRequest;
import com.github.ljtfreitas.julian.http.client.HTTPClientResponse;

public class DebugHTTPClient implements HTTPClient {

    private static final System.Logger log = System.getLogger("DebugHTTPClient");

    private final HTTPClient source;

    public DebugHTTPClient(HTTPClient source) {
        this.source = source;
    }

    @Override
    public HTTPClientRequest request(HTTPRequest<?> request) {
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

    private HTTPRequest<?> info(HTTPRequest<?> request) {
        request.body().ifPresentOrElse(body -> info(request, body), () -> info(request, ""));
        return request;
    }

    private void info(HTTPRequest<?> request, HTTPRequestBody body) {
        body.serialize().subscribe(new Subscriber<>() {

            private final ByteBuffer buffer = ByteBuffer.allocate(1024 * 100);

            @Override
            public void onSubscribe(Subscription subscription) {
                subscription.request(Integer.MAX_VALUE);
            }

            @Override
            public void onNext(ByteBuffer buffer) {
                this.buffer.put(buffer);
            }

            @Override
            public void onError(Throwable throwable) {
            }

            @Override
            public void onComplete() {
                String bodyAsString = new String(buffer.array());

                info(request, bodyAsString);
            }
        });
    }

    private void info(HTTPRequest<?> request, String body) {
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
