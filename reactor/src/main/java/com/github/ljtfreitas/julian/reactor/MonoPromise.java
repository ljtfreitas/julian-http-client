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
import com.github.ljtfreitas.julian.Subscriber;
import reactor.core.publisher.Mono;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public class MonoPromise<T> implements Promise<T> {

    private final Mono<T> mono;

    public MonoPromise(Mono<T> mono) {
        this.mono = mono;
    }

    @Override
    public MonoPromise<T> onSuccess(Consumer<? super T> fn) {
        return new MonoPromise<>(mono.doOnNext(fn));
    }

    @Override
    public <R> MonoPromise<R> then(Function<? super T, R> fn) {
        return new MonoPromise<>(mono.map(fn));
    }

    @Override
    public <R> MonoPromise<R> bind(Function<? super T, Promise<R>> fn) {
        return new MonoPromise<>(mono.flatMap(value -> fn.andThen(that -> that.cast(new Kind<MonoPromise<R>>(){})
                        .map(MonoPromise::mono)
                        .orElseGet(() -> Mono.fromCompletionStage(that.future())))
                        .apply(value)));
    }

    @Override
    public <R> Promise<R> fold(Function<? super T, R> success, Function<? super Exception, R> failure) {
        return new MonoPromise<>(mono.map(success).onErrorResume(e -> Mono.just(failure.apply((Exception) e))));
    }

    @Override
    public MonoPromise<T> recover(Function<? super Exception, T> fn) {
        return new MonoPromise<>(mono.onErrorResume(e -> Mono.just(fn.apply((Exception) e))));
    }

    @Override
    public <Err extends Exception> MonoPromise<T> recover(Class<? extends Err> expected, Function<? super Err, T> fn) {
        return new MonoPromise<>(mono.onErrorResume(expected, e -> Mono.just(fn.apply(e))));
    }

    @Override
    public <Err extends Exception> MonoPromise<T> failure(Function<? super Exception, Err> fn) {
        return new MonoPromise<>(mono.onErrorMap(t -> fn.apply((Exception) t)));
    }

    @Override
    public Promise<T> recover(Predicate<? super Exception> p, Function<? super Exception, T> fn) {
        return new MonoPromise<>(mono.onErrorResume(t -> p.test((Exception) t), e -> Mono.just(fn.apply((Exception) e))));
    }

    @Override
    public MonoPromise<T> onFailure(Consumer<? super Exception> fn) {
        return new MonoPromise<>(mono.doOnError(t -> fn.accept((Exception) t)));
    }

    @Override
    public Except<T> join() {
        return Except.run(mono::block);
    }

    @Override
    public CompletableFuture<T> future() {
        return mono.toFuture();
    }

    @Override
    public MonoPromise<T> subscribe(Subscriber<? super T> subscriber) {
        mono.subscribe(subscriber::success, e -> subscriber.failure((Exception) e), subscriber::done);
        return this;
    }

    public Mono<T> mono() {
        return mono;
    }
}
