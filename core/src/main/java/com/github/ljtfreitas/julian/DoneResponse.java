package com.github.ljtfreitas.julian;

import java.util.function.Function;
import java.util.function.Predicate;

class DoneResponse<T> implements Response<T> {

    private final T value;

    DoneResponse(T value) {
        this.value = value;
    }

    @Override
    public T body() {
        return value;
    }

    @Override
    public <R> Response<R> map(Function<? super T, R> fn) {
        return new DoneResponse<>(fn.apply(value));
    }

    @Override
    public <E extends Exception> Response<T> recover(Function<? super E, T> fn) {
        return this;
    }

    @Override
    public <E extends Exception> Response<T> recover(Class<? super E> expected, Function<? super E, T> fn) {
        return this;
    }

    @Override
    public <E extends Exception> Response<T> recover(Predicate<? super E> p, Function<? super E, T> fn) {
        return this;
    }
}
