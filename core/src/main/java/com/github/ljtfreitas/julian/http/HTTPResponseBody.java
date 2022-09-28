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

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow.Publisher;
import java.util.concurrent.Flow.Subscription;
import java.util.function.Function;
import java.util.function.Supplier;

public interface HTTPResponseBody {

    <T> Optional<CompletableFuture<T>> readAsInputStream(Function<InputStream, T> fn);

    <T> Optional<CompletableFuture<T>> readAsBytes(Function<byte[], T> fn);

    Optional<Publisher<List<ByteBuffer>>> content();

    static HTTPResponseBody empty() {
        return new EmptyHTTPResponseBody();
    }

    static HTTPResponseBody optional(HTTPStatus status, HTTPHeaders headers, Supplier<HTTPResponseBody> body) {
        return new OptionalHTTPResponseBody(status, headers, body);
    }

    static HTTPResponseBody lazy(Publisher<List<ByteBuffer>> publisher) {
        return new PublisherHTTPResponseBody(publisher);
    }

    static HTTPResponseBody some(byte[] bodyAsBytes) {
        return new PublisherHTTPResponseBody(subscriber -> subscriber.onSubscribe(new Subscription() {

            private final ByteBuffer bodyAsBuffer = ByteBuffer.wrap(bodyAsBytes);

            private long requested = 0;

            @Override
            public void request(long n) {
                if (!bodyAsBuffer.hasRemaining()) {
                    subscriber.onComplete();

                } else if (n > bodyAsBuffer.remaining()) {
                    subscriber.onNext(List.of(bodyAsBuffer));
                    subscriber.onComplete();

                } else {
                    int size = (int) n;
                    byte[] bytes = new byte[size];
                    bodyAsBuffer.get(bytes);
                    subscriber.onNext(List.of(ByteBuffer.wrap(bytes)));
                }
            }

            @Override
            public void cancel() {}
        }));
    }
}
