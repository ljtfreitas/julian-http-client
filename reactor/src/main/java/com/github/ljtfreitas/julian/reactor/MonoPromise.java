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

import com.github.ljtfreitas.julian.Attempt;
import com.github.ljtfreitas.julian.Kind;
import com.github.ljtfreitas.julian.Promise;
import com.github.ljtfreitas.julian.Subscriber;
import reactor.core.publisher.Mono;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
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
    public <T2, R> MonoPromise<R> zip(Promise<T2> other, BiFunction<? super T, ? super T2, R> fn) {
        Mono<T2> m2 = other.cast(new Kind<MonoPromise<T2>>() {})
                .map(MonoPromise::mono)
                .orElseGet(() -> Mono.fromCompletionStage(other.future()));

        return new MonoPromise<>(mono.zipWith(m2).map(t -> fn.apply(t.getT1(), t.getT2())));
    }

    @Override
    public <R> R fold(Function<? super T, R> success, Function<? super Throwable, R> failure) {
        return mono.map(success)
                .onErrorResume(e -> Mono.just(failure.apply((Exception) e)))
                .block();
    }

    @Override
    public MonoPromise<T> recover(Function<? super Throwable, T> fn) {
        return new MonoPromise<>(mono.onErrorResume(e -> Mono.just(fn.apply(e))));
    }

    @Override
    public <Err extends Throwable> MonoPromise<T> recover(Class<? extends Err> expected, Function<? super Err, T> fn) {
        return new MonoPromise<>(mono.onErrorResume(expected, e -> Mono.just(fn.apply(e))));
    }

    @Override
    public <Err extends Throwable> MonoPromise<T> failure(Function<? super Throwable, Err> fn) {
        return new MonoPromise<>(mono.onErrorMap(fn));
    }

    @Override
    public Promise<T> recover(Predicate<? super Throwable> p, Function<? super Throwable, T> fn) {
        return new MonoPromise<>(mono.onErrorResume(p, e -> Mono.just(fn.apply(e))));
    }

    @Override
    public MonoPromise<T> onFailure(Consumer<? super Throwable> fn) {
        return new MonoPromise<>(mono.doOnError(fn));
    }

    @Override
    public Attempt<T> join() {
        return Attempt.run(mono::block);
    }

    @Override
    public CompletableFuture<T> future() {
        return mono.toFuture();
    }

    @Override
    public MonoPromise<T> subscribe(Subscriber<? super T> subscriber) {
        mono.subscribe(subscriber::success, subscriber::failure, subscriber::done);
        return this;
    }

    public Mono<T> mono() {
        return mono;
    }
}
