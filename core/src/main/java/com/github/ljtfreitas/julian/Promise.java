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

package com.github.ljtfreitas.julian;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static java.util.concurrent.CompletableFuture.supplyAsync;

public interface Promise<T> {

    Promise<T> onSuccess(Consumer<? super T> fn);

	<R> Promise<R> then(Function<? super T, R> fn);

	<R> Promise<R> bind(Function<? super T, Promise<R>> fn);

	<T2, R> Promise<R> zip(Promise<T2> other, BiFunction<? super T, ? super T2, R> fn);

	Attempt<T> join();

	Promise<T> onFailure(Consumer<? super Exception> fn);

	<Err extends Exception> Promise<T> failure(Function<? super Exception, Err> fn);

	Promise<T> recover(Function<? super Exception, T> fn);

	Promise<T> recover(Predicate<? super Exception> p, Function<? super Exception, T> fn);

	<Err extends Exception> Promise<T> recover(Class<? extends Err> expected, Function<? super Err, T> fn);

	<R> R fold(Function<? super T, R> success, Function<? super Exception, R> failure);

	CompletableFuture<T> future();

	Promise<T> subscribe(Subscriber<? super T> subscriber);

	@SuppressWarnings("unchecked")
	default <Err extends Exception, P extends Promise<T>> Optional<P> cast(Kind<P> candidate) {
		Class<?> javaType = candidate.javaType().rawClassType();
		return javaType.isAssignableFrom(this.getClass()) ? Optional.of((P) javaType.cast(this)) : Optional.empty();
	}

    static <T> Promise<T> done(T value) {
		return new DonePromise<>(value);
	}

	static <T, E extends Exception> Promise<T> failed(E e) {
		return new FailedPromise<>(e);
	}

	static <T> Promise<T> empty() {
		return new DonePromise<>(null);
	}

	static <T> Promise<T> pending(CompletableFuture<T> future) {
		return new DefaultPromise<>(future);
	}

	static <T> Promise<T> pending(Supplier<T> fn) {
		return new DefaultPromise<>(supplyAsync(fn));
	}

	static <T> Promise<T> pending(Supplier<T> fn, Executor executor) {
		CompletableFuture<T> future = executor == null ? supplyAsync(fn) : PromiseCompletableFuture.pending(fn, executor);
		return new DefaultPromise<>(future);
	}

}
