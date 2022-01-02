package com.github.ljtfreitas.julian.http.client.reactor;

import com.github.ljtfreitas.julian.Except;
import com.github.ljtfreitas.julian.Promise;
import reactor.core.publisher.Mono;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

class MonoPromise<T, E extends Exception> implements Promise<T, E> {

    private final Mono<T> mono;

    MonoPromise(Mono<T> mono) {
        this.mono = mono;
    }

    @Override
    public Promise<T, E> onSuccess(Consumer<T> fn) {
        return new MonoPromise<>(mono.doOnNext(fn));
    }

    @Override
    public <R> Promise<R, E> then(Function<? super T, R> fn) {
        return new MonoPromise<>(mono.map(fn));
    }

    @Override
    public <R, Err extends Exception> Promise<R, Err> bind(Function<? super T, Promise<R, Err>> fn) {
        return new MonoPromise<>(mono.flatMap(value -> fn.andThen(Promise::future)
                .andThen(Mono::fromCompletionStage)
                .apply(value)));
    }

    @SuppressWarnings("unchecked")
    @Override
    public Promise<T, E> recover(Function<? super E, T> fn) {
        return new MonoPromise<>(mono.onErrorResume(e -> Mono.just(fn.apply((E) e))));
    }

    @Override
    public <Err extends E> Promise<T, E> recover(Class<? extends Err> expected, Function<? super Err, T> fn) {
        return new MonoPromise<>(mono.onErrorResume(expected, e -> Mono.just(fn.apply(e))));
    }

    @SuppressWarnings("unchecked")
    @Override
    public <Err extends Exception> Promise<T, Err> failure(Function<? super E, Err> fn) {
        return new MonoPromise<>(mono.onErrorMap(t -> fn.apply((E) t)));
    }

    @SuppressWarnings("unchecked")
    @Override
    public Promise<T, E> recover(Predicate<? super E> p, Function<? super E, T> fn) {
        return new MonoPromise<>(mono.onErrorResume(t -> p.test((E) t), e -> Mono.just(fn.apply((E) e))));
    }

    @SuppressWarnings("unchecked")
    @Override
    public Promise<T, E> onFailure(Consumer<? super E> fn) {
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
}
