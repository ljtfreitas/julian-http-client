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

package com.github.ljtfreitas.julian.reactor;

import com.github.ljtfreitas.julian.Except;
import com.github.ljtfreitas.julian.Kind;
import com.github.ljtfreitas.julian.Promise;
import reactor.core.publisher.Mono;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public class MonoPromise<T, E extends Exception> implements Promise<T, E> {

    private final Mono<T> mono;

    public MonoPromise(Mono<T> mono) {
        this.mono = mono;
    }

    @Override
    public MonoPromise<T, E> onSuccess(Consumer<T> fn) {
        return new MonoPromise<>(mono.doOnNext(fn));
    }

    @Override
    public <R> MonoPromise<R, E> then(Function<? super T, R> fn) {
        return new MonoPromise<>(mono.map(fn));
    }

    @Override
    public <R, Err extends Exception> MonoPromise<R, Err> bind(Function<? super T, Promise<R, Err>> fn) {
        return new MonoPromise<>(mono.flatMap(value -> fn.andThen(that -> that.cast(new Kind<MonoPromise<R, Err>>(){})
                        .map(MonoPromise::mono)
                        .orElseGet(() -> Mono.fromCompletionStage(that.future())))
                        .apply(value)));
    }

    @SuppressWarnings("unchecked")
    @Override
    public MonoPromise<T, E> recover(Function<? super E, T> fn) {
        return new MonoPromise<>(mono.onErrorResume(e -> Mono.just(fn.apply((E) e))));
    }

    @Override
    public <Err extends E> MonoPromise<T, E> recover(Class<? extends Err> expected, Function<? super Err, T> fn) {
        return new MonoPromise<>(mono.onErrorResume(expected, e -> Mono.just(fn.apply(e))));
    }

    @SuppressWarnings("unchecked")
    @Override
    public <Err extends Exception> MonoPromise<T, Err> failure(Function<? super E, Err> fn) {
        return new MonoPromise<>(mono.onErrorMap(t -> fn.apply((E) t)));
    }

    @SuppressWarnings("unchecked")
    @Override
    public Promise<T, E> recover(Predicate<? super E> p, Function<? super E, T> fn) {
        return new MonoPromise<>(mono.onErrorResume(t -> p.test((E) t), e -> Mono.just(fn.apply((E) e))));
    }

    @SuppressWarnings("unchecked")
    @Override
    public MonoPromise<T, E> onFailure(Consumer<? super E> fn) {
        return new MonoPromise<>(mono.doOnError(t -> fn.accept((E) t)));
    }

    @Override
    public Except<T> join() {
        return Except.run(mono::block);
    }

    @Override
    public CompletableFuture<T> future() {
        return mono.toFuture();
    }

    @SuppressWarnings("unchecked")
    @Override
    public MonoPromise<T, E> subscribe(Subscriber<? super T, ? super E> subscriber) {
        mono.subscribe(subscriber::success, e -> subscriber.failure((E) e), subscriber::done);
        return this;
    }

    public Mono<T> mono() {
        return mono;
    }
}
