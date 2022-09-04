package com.github.ljtfreitas.julian;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

class DoneResponse<T> implements Response<T> {

    private final T value;

    DoneResponse(T value) {
        this.value = value;
    }

    @Override
    public Attempt<T> body() {
        return Attempt.success(value);
    }

    @Override
    public <R> Response<R> map(Function<? super T, R> fn) {
        return new DoneResponse<>(fn.apply(value));
    }

    @Override
    public Response<T> onSuccess(Consumer<? super T> fn) {
        fn.accept(value);
        return this;
    }

    @Override
    public Response<T> onFailure(Consumer<? super Throwable> fn) {
        return this;
    }

    @Override
    public Response<T> recover(Predicate<? super Throwable> p, Function<? super Throwable, T> fn) {
        return this;
    }

    @Override
    public Response<T> recover(Function<? super Throwable, T> fn) {
        return this;
    }

    @Override
    public <Err extends Throwable> Response<T> recover(Class<? extends Err> expected, Function<? super Err, T> fn) {
        return this;
    }

    @Override
    public <R> R fold(Function<? super T, R> success, Function<? super Throwable, R> failure) {
        return success.apply(value);
    }

    @Override
    public Response<T> subscribe(Subscriber<? super T> subscriber) {
        subscriber.success(value);
        subscriber.done();
        return this;
    }
}
