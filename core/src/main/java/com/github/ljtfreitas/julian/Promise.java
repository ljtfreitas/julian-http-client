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

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static java.util.concurrent.CompletableFuture.supplyAsync;

public interface Promise<T, E extends Exception> {

	Promise<T, E> onSuccess(Consumer<T> fn);

	<R> Promise<R, E> then(Function<? super T, R> fn);

	<R, Err extends Exception> Promise<R, Err> bind(Function<? super T, Promise<R, Err>> fn);

	Except<T> join();

	Promise<T, E> onFailure(Consumer<? super E> fn);

	<Err extends Exception> Promise<T, Err> failure(Function<? super E, Err> fn);

	Promise<T, E> recover(Function<? super E, T> fn);

	Promise<T, E> recover(Predicate<? super E> p, Function<? super E, T> fn);

	<Err extends E> Promise<T, E> recover(Class<? extends Err> expected, Function<? super Err, T> fn);

	CompletableFuture<T> future();

    static <T, E extends Exception> Promise<T, E> done(T value) {
		return new DonePromise<>(value);
	}

	static <T, E extends Exception> Promise<T, E> failed(E e) {
		return new FailedPromise<>(e);
	}

	static <T, E extends Exception> Promise<T, E> pending(CompletableFuture<T> future) {
		return new DefaultPromise<>(future);
	}

	static <T, E extends Exception> Promise<T, E> pending(Supplier<T> fn) {
		return new DefaultPromise<>(supplyAsync(fn));
	}
}
