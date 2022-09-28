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

package com.github.ljtfreitas.julian.http.client;

import com.github.ljtfreitas.julian.Promise;
import com.github.ljtfreitas.julian.Response;
import com.github.ljtfreitas.julian.http.HTTPHeaders;
import com.github.ljtfreitas.julian.http.HTTPRequestBody;
import com.github.ljtfreitas.julian.http.HTTPRequestDefinition;
import com.github.ljtfreitas.julian.http.HTTPResponseBody;
import com.github.ljtfreitas.julian.http.HTTPStatus;

import java.io.ByteArrayOutputStream;
import java.lang.System.Logger.Level;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow.Processor;
import java.util.concurrent.Flow.Publisher;
import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.Flow.Subscription;
import java.util.concurrent.SubmissionPublisher;
import java.util.function.Function;

import static java.util.function.Function.identity;

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

    private HTTPRequestDefinition info(HTTPRequestDefinition request) {
        request.body().ifPresentOrElse(body -> info(request, body), () -> info(request, ""));
        return request;
    }

    private void info(HTTPRequestDefinition request, HTTPRequestBody body) {
        body.serialize().subscribe(new Subscriber<>() {

            private Subscription subscription;

            private final Collection<ByteBuffer> received = new ArrayList<>();

            @Override
            public void onSubscribe(Subscription subscription) {
                this.subscription = subscription;
                subscription.request(1);
            }

            @Override
            public void onNext(ByteBuffer buffer) {
                received.add(buffer);
                subscription.request(1);
            }

            @Override
            public void onError(Throwable throwable) {
                log.log(Level.ERROR, throwable.getMessage(), throwable);
            }

            @Override
            public void onComplete() {
                int size = received.stream().mapToInt(Buffer::remaining).sum();

                ByteBuffer buffer = ByteBuffer.allocate(size);
                received.forEach(buffer::put);

                String bodyAsString = new String(buffer.array());

                info(request, bodyAsString);
            }
        });
    }

    private void info(HTTPRequestDefinition request, String body) {
        String headers = request.headers().toString();

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

    private class DebugHTTPClientRequest implements HTTPClientRequest  {

        private final HTTPClientRequest source;

        public DebugHTTPClientRequest(HTTPClientRequest source) {
            this.source = source;
        }

        @Override
        public Promise<HTTPClientResponse> execute() {
            return source.execute().then(this::info);
        }

        private HTTPClientResponse info(HTTPClientResponse response) {
            byte[] bodyAsBytes = response.body().readAsBytes(identity()).map(CompletableFuture::join).orElse(new byte[0]);

            info(response, bodyAsBytes);

            return new HTTPClientResponse() {

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
                    return HTTPResponseBody.some(bodyAsBytes);
                }

                @Override
                public <T, R extends Response<T, ? extends Throwable>> Optional<R> success(Function<? super HTTPClientResponse, R> fn) {
                    return response.success(r -> fn.apply(this));
                }

                @Override
                public <T, R extends Response<T, ? extends Throwable>> Optional<R> failure(Function<? super HTTPClientResponse, R> fn) {
                    return response.failure(r -> fn.apply(this));
                }
            };
        }

        private void info(HTTPClientResponse response, byte[] bodyAsBytes) {
            String headers = response.headers().toString();
            String body = new String(bodyAsBytes);

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
        }
    }

    private class DebugHTTPClientResponse implements HTTPClientResponse {

        private final HTTPClientResponse source;
        private final HTTPResponseBody body;

        private DebugHTTPClientResponse(HTTPClientResponse source) {
            this.source = source;
            this.body = source.body().content()
                    .map(publisher -> HTTPResponseBody.lazy(DebugHTTPResponsePublisher.process(publisher, this)))
                    .orElseGet(HTTPResponseBody::empty);
        }

        @Override
        public HTTPStatus status() {
            return source.status();
        }

        @Override
        public HTTPHeaders headers() {
            return source.headers();
        }

        @Override
        public HTTPResponseBody body() {
            return body;
        }

        @Override
        public <T, R extends Response<T, ? extends Throwable>> Optional<R> success(Function<? super HTTPClientResponse, R> fn) {
            return source.success(r -> fn.apply(this));
        }

        @Override
        public <T, R extends Response<T, ? extends Throwable>> Optional<R> failure(Function<? super HTTPClientResponse, R> fn) {
            return source.failure(r -> fn.apply(this));
        }
    }

    private static class DebugHTTPResponsePublisher
            extends SubmissionPublisher<List<ByteBuffer>>
            implements Processor<List<ByteBuffer>, List<ByteBuffer>> {

        private final HTTPClientResponse response;

        private final List<byte[]> allBytes = new ArrayList<>();

        private Subscription subscription = null;

        public DebugHTTPResponsePublisher(HTTPClientResponse response) {
            this.response = response;
        }

        @Override
        public void onSubscribe(Subscription subscription) {
            this.subscription = subscription;
            this.subscription.request(Long.MAX_VALUE);
        }

        @Override
        public void onNext(List<ByteBuffer> buffers) {
            byte[] bufferAsBytes = joinBytes(buffers);
            allBytes.add(bufferAsBytes);
            submit(List.of(ByteBuffer.wrap(bufferAsBytes)));
            subscription.request(Long.MAX_VALUE);
        }

        private byte[] joinBytes(List<ByteBuffer> buffers) {
            int expectedSize = buffers.stream().mapToInt(Buffer::remaining).sum();
            byte[] allBytes = new byte[expectedSize];

            buffers.stream().reduce(0,
                    (f, b) -> { int l = b.remaining(); b.get(allBytes, f, l); return f + l; },
                    (a, b) -> b);

            return allBytes;
        }

        @Override
        public void onError(Throwable throwable) {
            log.log(Level.ERROR, throwable.getMessage(), throwable);
            subscription.cancel();
        }

        @Override
        public void onComplete() {
            String headers = response.headers().toString();
            String body = new String(bodyAsBytes());

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

            close();
        }

        private byte[] bodyAsBytes() {
            return allBytes.stream().reduce(new ByteArrayOutputStream(),
                    (output, bytes) -> { output.write(bytes, 0, bytes.length); return output; },
                    (a, b) -> b).toByteArray();
        }

        private static DebugHTTPResponsePublisher process(Publisher<List<ByteBuffer>> publisher, HTTPClientResponse source) {
            DebugHTTPResponsePublisher processor = new DebugHTTPResponsePublisher(source);
            publisher.subscribe(processor);
            return processor;
        }
    }
}
