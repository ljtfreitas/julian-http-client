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

class FailedPromise<T, E extends Exception> implements Promise<T, E> {

	private final E failure;

	FailedPromise(E failure) {
		this.failure = failure;
	}

	@Override
	public Promise<T, E> onSuccess(Consumer<T> fn) {
		return this;
	}

	@Override
	public <R> Promise<R, E> then(Function<? super T, R> fn) {
		return new FailedPromise<>(failure);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <R, Err extends Exception> Promise<R, Err> bind(Function<? super T, Promise<R, Err>> fn) {
		return new FailedPromise<>((Err) failure);
	}

	@Override
	public Promise<T, E> recover(Function<? super E, T> fn) {
		return new DonePromise<>(fn.apply(failure));
	}

	@SuppressWarnings("unchecked")
	@Override
	public <Err extends E> Promise<T, E> recover(Class<? extends Err> expected, Function<? super Err, T> fn) {
		Exception cause = deep(failure);

		if (expected.isInstance(cause))
			return new DonePromise<>(fn.apply((Err) failure));
		else
			return this;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Promise<T, E> recover(Predicate<? super E> p, Function<? super E, T> fn) {
		Exception cause = deep(failure);

		if (cause != null && p.test((E) cause))
			return new DonePromise<>(fn.apply(failure));
		else
			return this;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <Err extends Exception> Promise<T, Err> failure(Function<? super E, Err> fn) {
		Exception cause = deep(failure);

		if (cause != null)
			return new FailedPromise<>(fn.apply((E) cause));
		else
			return new FailedPromise<>((Err) failure);
	}

	@Override
	public Promise<T, E> onFailure(Consumer<? super E> fn) {
		fn.accept(failure);
		return this;
	}

	private Exception deep(Throwable t) {
		if (t instanceof CompletionException || t instanceof ExecutionException) return deep(t.getCause()); else return (Exception) t;
	}

	@Override
	public Except<T> join() {
		return Except.failed(failure);
	}

	@Override
	public CompletableFuture<T> future() {
		return CompletableFuture.failedFuture(failure);
	}

	@Override
	public Promise<T, E> subscribe(Subscriber<? super T, ? super E> subscriber) {
		subscriber.failure(failure);
		subscriber.done();
		return this;
	}
}
