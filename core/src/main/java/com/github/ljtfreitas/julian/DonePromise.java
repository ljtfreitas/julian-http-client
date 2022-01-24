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
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

class DonePromise<T, E extends Exception> implements Promise<T, E> {

	private final T value;

	DonePromise(T value) {
		this.value = value;
	}

	@Override
	public Promise<T, E> onSuccess(Consumer<T> fn) {
		fn.accept(value);
		return this;
	}

	@Override
	public <R> Promise<R, E> then(Function<? super T, R> fn) {
		return new DonePromise<>(fn.apply(value));
	}

	@Override
	public <R, Err extends Exception> Promise<R, Err> bind(Function<? super T, Promise<R, Err>> fn) {
		return fn.apply(value);
	}

	@Override
	public Promise<T, E> recover(Function<? super E, T> fn) {
		return this;
	}

	@Override
	public <Err extends E> Promise<T, E> recover(Class<? extends Err> expected, Function<? super Err, T> fn) {
		return this;
	}

	@Override
	public Promise<T, E> recover(Predicate<? super E> p, Function<? super E, T> fn) {
		return this;
	}

	@Override
	public <Err extends Exception> Promise<T, Err> failure(Function<? super E, Err> fn) {
		return new DonePromise<>(value);
	}

	@Override
	public Promise<T, E> onFailure(Consumer<? super E> fn) {
		return this;
	}

	@Override
	public Except<T> join() {
		return Except.success(value);
	}

	@Override
	public CompletableFuture<T> future() {
		return CompletableFuture.completedFuture(value);
	}

	@Override
	public Promise<T, E> subscribe(Subscriber<? super T, ? super E> subscriber) {
		subscriber.success(value);
		subscriber.done();
		return this;
	}
}
