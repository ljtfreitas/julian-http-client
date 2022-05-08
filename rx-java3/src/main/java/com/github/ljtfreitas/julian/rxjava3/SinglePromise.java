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

package com.github.ljtfreitas.julian.rxjava3;

import com.github.ljtfreitas.julian.Attempt;
import io.reactivex.rxjava3.core.Single;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import com.github.ljtfreitas.julian.Kind;
import com.github.ljtfreitas.julian.Promise;
import com.github.ljtfreitas.julian.Subscriber;

public class SinglePromise<T> implements Promise<T> {

    private final Single<T> single;

    public SinglePromise(Single<T> single) {
        this.single = single;
    }

    @Override
    public SinglePromise<T> onSuccess(Consumer<? super T> fn) {
        return new SinglePromise<>(single.doOnSuccess(fn::accept));
    }

    @Override
    public <R> SinglePromise<R> then(Function<? super T, R> fn) {
        return new SinglePromise<>(single.map(fn::apply));
    }

    @Override
    public <R> SinglePromise<R> bind(Function<? super T, Promise<R>> fn) {
        return new SinglePromise<>(single.flatMap(value -> fn.andThen(that -> that.cast(new Kind<SinglePromise<R>>(){})
                        .map(SinglePromise::single)
                        .orElseGet(() -> Single.fromCompletionStage(that.future())))
                        .apply(value)));
    }

    @Override
    public <T2, R> SinglePromise<R> zip(Promise<T2> other, BiFunction<? super T, ? super T2, R> fn) {
        Single<T2> m2 = other.cast(new Kind<SinglePromise<T2>>() {})
                .map(SinglePromise::single)
                .orElseGet(() -> Single.fromCompletionStage(other.future()));

        return new SinglePromise<>(single.zipWith(m2, fn::apply));
    }

    @Override
    public <R> R fold(Function<? super T, R> success, Function<? super Exception, R> failure) {
        return single.map(success::apply)
                .onErrorReturn(e -> failure.apply((Exception) e))
                .blockingGet();
    }

    @Override
    public SinglePromise<T> recover(Function<? super Exception, T> fn) {
        return new SinglePromise<>(single.onErrorReturn(e -> fn.apply((Exception) e)));
    }

    @SuppressWarnings("unchecked")
    @Override
    public <Err extends Exception> SinglePromise<T> recover(Class<? extends Err> expected, Function<? super Err, T> fn) {
        return new SinglePromise<>(single
                .onErrorResumeNext(e -> (expected.isInstance(e)) ? Single.just(fn.apply((Err) e)) : Single.never()));
    }

    @Override
    public <Err extends Exception> SinglePromise<T> failure(Function<? super Exception, Err> fn) {
        return new SinglePromise<>(single.onErrorResumeNext(t -> Single.error(fn.apply((Exception) t))));
    }

    @Override
    public Promise<T> recover(Predicate<? super Exception> p, Function<? super Exception, T> fn) {
        return new SinglePromise<>(single
                .onErrorResumeNext(t -> p.test((Exception) t) ? Single.just(fn.apply((Exception) t)) : Single.never()));
    }

    @Override
    public SinglePromise<T> onFailure(Consumer<? super Exception> fn) {
        return new SinglePromise<>(single.doOnError(t -> fn.accept((Exception) t)));
    }

    @Override
    public Attempt<T> join() {
        return Attempt.run(single::blockingGet);
    }

    @Override
    public CompletableFuture<T> future() {
        CompletableFuture<T> future = new CompletableFuture<>();
        single.subscribe(future::complete, future::completeExceptionally);
        return future;
    }

    @Override
    public SinglePromise<T> subscribe(Subscriber<? super T> subscriber) {
        single.subscribe(value -> { subscriber.success(value); subscriber.done(); },
                e -> subscriber.failure((Exception) e));
        return this;
    }

    public Single<T> single() {
        return single;
    }
}
