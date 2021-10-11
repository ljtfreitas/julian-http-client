package com.github.ljtfreitas.julian.http.client.reactor;

import com.github.ljtfreitas.julian.Except;
import com.github.ljtfreitas.julian.Promise;
import reactor.core.publisher.Mono;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

class MonoPromise<T> implements Promise<T> {

    private final Mono<T> mono;

    MonoPromise(Mono<T> mono) {
        this.mono = mono;
    }

    @Override
    public <R> Promise<R> then(Function<? super T, R> fn) {
        return new MonoPromise<>(mono.map(fn));
    }

    @Override
    public <R> Promise<R> bind(Function<? super T, Promise<R>> fn) {
        return new MonoPromise<>(mono.flatMap(value -> fn.andThen(Promise::future)
                .andThen(Mono::fromCompletionStage)
                .apply(value)));
    }

    @Override
    public Promise<T> failure(Function<Throwable, ? extends Exception> fn) {
        return new MonoPromise<>(mono.onErrorMap(fn));
    }

    @Override
    public Except<T> join() {
        return Except.run(mono::block);
    }

    @Override
    public CompletableFuture<T> future() {
        return mono.toFuture();
    }
}
