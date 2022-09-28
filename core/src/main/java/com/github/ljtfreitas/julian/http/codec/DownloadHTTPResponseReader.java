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

package com.github.ljtfreitas.julian.http.codec;

import com.github.ljtfreitas.julian.Attempt;
import com.github.ljtfreitas.julian.Disposable;
import com.github.ljtfreitas.julian.JavaType;
import com.github.ljtfreitas.julian.Promise;
import com.github.ljtfreitas.julian.Subscriber;
import com.github.ljtfreitas.julian.http.Download;
import com.github.ljtfreitas.julian.http.HTTPResponseBody;
import com.github.ljtfreitas.julian.http.MediaType;

import java.io.InputStream;
import java.net.http.HttpResponse.BodySubscriber;
import java.net.http.HttpResponse.BodySubscribers;
import java.nio.ByteBuffer;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;
import java.util.concurrent.Flow.Publisher;

import static java.util.function.Function.identity;

public class DownloadHTTPResponseReader implements HTTPResponseReader<Download> {

    private static final DownloadHTTPResponseReader SINGLE_INSTANCE = new DownloadHTTPResponseReader();

    private static final Disposable EMPTY_DISPOSABLE = () -> Attempt.just(null);

    @Override
    public Collection<MediaType> contentTypes() {
        return WildcardHTTPMessageCodec.WILDCARD_MEDIA_TYPES;
    }

    @Override
    public boolean readable(MediaType candidate, JavaType javaType) {
        return supports(candidate) && javaType.is(Download.class);
    }

    @Override
    public Optional<CompletableFuture<Download>> read(HTTPResponseBody body, JavaType javaType) {
        return body.content()
                .map(publisher -> CompletableFuture.completedFuture(new HTTPResponseContentDownload(body)));
    }

    static class HTTPResponseContentDownload implements Download {

        private final HTTPResponseBody body;

        public HTTPResponseContentDownload(HTTPResponseBody body) {
            this.body = body;
        }

        @Override
        public Promise<InputStream> readAsInputStream() {
            return body.readAsInputStream(identity()).map(Promise::pending)
                    .orElseGet(() -> Promise.done(InputStream.nullInputStream()));
        }

        @Override
        public Promise<byte[]> readAsBytes() {
            return body.readAsBytes(identity()).map(Promise::pending)
                    .orElseGet(() -> Promise.done(new byte[0]));
        }

        @Override
        public Promise<ByteBuffer> readAsBuffer() {
            return body.readAsBytes(ByteBuffer::wrap).map(Promise::pending)
                    .orElseGet(() -> Promise.done(ByteBuffer.wrap(new byte[0])));
        }

        @Override
        public Disposable subscribe(Subscriber<List<ByteBuffer>, Throwable> subscriber) {
            return body.content().map(publisher -> subscribedDisposable(publisher, flowSubscriber(subscriber)))
                    .orElse(EMPTY_DISPOSABLE);
        }

        private Flow.Subscriber<List<ByteBuffer>> flowSubscriber(Subscriber<List<ByteBuffer>, Throwable> subscriber) {
            Flow.Subscriber<List<ByteBuffer>> s = new Flow.Subscriber<>() {

                private Flow.Subscription subscription = null;

                @Override
                public void onSubscribe(Flow.Subscription subscription) {
                    if (this.subscription != null) {
                        subscription.cancel();
                    } else {
                        this.subscription = subscription;
                        this.subscription.request(1);
                    }
                }

                @Override
                public void onNext(List<ByteBuffer> item) {
                    Attempt.just(() -> subscriber.success(item))
                            .onFailure(e -> {
                                subscription.cancel();
                                onError(e);
                            });
                }

                @Override
                public void onError(Throwable throwable) {
                    subscriber.failure(throwable);
                }

                @Override
                public void onComplete() {
                    subscriber.done();
                }
            };

            return BodySubscribers.buffering(BodySubscribers.fromSubscriber(s), DisposableSubscriber.DEFAULT_BUFFER_SIZE);
        }

        @Override
        public Promise<Path> writeTo(Path file, OpenOption...openOptions) {
            return body.content().map(publisher -> subscribed(publisher, BodySubscribers.ofFile(file, openOptions)))
                    .map(Promise::pending)
                    .orElseGet(() -> Promise.done(file));
        }

        private <T> CompletableFuture<T> subscribed(Publisher<List<ByteBuffer>> publisher, BodySubscriber<T> subscriber) {
            publisher.subscribe(subscriber);
            return subscriber.getBody().toCompletableFuture();
        }

        private Disposable subscribedDisposable(Publisher<List<ByteBuffer>> publisher, Flow.Subscriber<List<ByteBuffer>> subscriber) {
            DisposableSubscriber<List<ByteBuffer>> s = new DisposableSubscriber<>(subscriber);
            BodySubscriber<Void> noneSubscriber = BodySubscribers.fromSubscriber(s);
            publisher.subscribe(noneSubscriber);
            return s;
        }
    }

    private static class DisposableSubscriber<T> implements Disposable, Flow.Subscriber<T> {

        private static final int DEFAULT_BUFFER_SIZE = 1024 * 64; // 64kb

        private final Flow.Subscriber<T> downstream;

        private Flow.Subscription subscription;

        private DisposableSubscriber(Flow.Subscriber<T> downstream) {
            this.downstream = downstream;
        }

        @Override
        public Attempt<Void> dispose() {
            return Optional.ofNullable(subscription)
                    .map(s -> Attempt.just(subscription::cancel))
                    .orElseGet(() -> Attempt.success(null));
        }

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            downstream.onSubscribe(subscription);
            this.subscription = subscription;
        }

        @Override
        public void onNext(T item) {
            downstream.onNext(item);
        }

        @Override
        public void onError(Throwable throwable) {
            downstream.onError(throwable);
        }

        @Override
        public void onComplete() {
            downstream.onComplete();
        }
    }

    public static DownloadHTTPResponseReader get() {
        return SINGLE_INSTANCE;
    }
}
