package com.github.ljtfreitas.julian;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

class DoneResponse<T, E extends Exception> implements Response<T, E> {

    private final T value;

    DoneResponse(T value) {
        this.value = value;
    }

    @Override
    public Except<T> body() {
        return Except.success(value);
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
    public Response<T, E> recover(Predicate<? super E> p, Function<? super E, T> fn) {
        return this;
    }

    @Override
    public Response<T, E> recover(Function<? super E, T> fn) {
        return this;
    }

    @Override
    public <Err extends E> Response<T, E> recover(Class<? extends Err> expected, Function<? super Err, T> fn) {
        return this;
    }

    @Override
    public <R> R fold(Function<T, R> success, Function<? super E, R> failure) {
        return success.apply(value);
    }
}
