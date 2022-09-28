package com.github.ljtfreitas.julian;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

class DoneResponse<T, E extends Throwable> implements Response<T, E> {

    private final T value;

    DoneResponse(T value) {
        this.value = value;
    }

    @Override
    public Attempt<T> body() {
        return Attempt.success(value);
    }

    @Override
    public <R> Response<R, E> map(Function<? super T, R> fn) {
        return new DoneResponse<>(fn.apply(value));
    }

    @Override
    public Response<T, E> onSuccess(Consumer<? super T> fn) {
        fn.accept(value);
        return this;
    }

    @Override
    public Response<T, E> onFailure(Consumer<? super E> fn) {
        return this;
    }

    @Override
    public Response<T, E> recover(Class<? extends E> expected, Function<? super E, T> fn) {
        return this;
    }

    @Override
    public Response<T, E> recover(Predicate<? super E> p, Function<? super E, T> fn) {
        return this;
    }

    @Override
    public Response<T, E> recover(Function<? super E, T> fn) {
        return this;
    }

    @Override
    public <R> R fold(Function<? super T, R> success, Function<? super E, R> failure) {
        return success.apply(value);
    }

    @Override
    public Response<T, E> subscribe(Subscriber<? super T, E> subscriber) {
        subscriber.success(value);
        subscriber.done();
        return this;
    }

    @Override
    public Promise<T> promise() {
        return Promise.done(value);
    }
}
