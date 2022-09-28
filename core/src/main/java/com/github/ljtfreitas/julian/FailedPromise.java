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
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

class FailedPromise<T> implements Promise<T> {

	private final Throwable failure;

	FailedPromise(Throwable failure) {
		this.failure = failure;
	}

	@Override
	public Promise<T> onSuccess(Consumer<? super T> fn) {
		return this;
	}

	@Override
	public <R> Promise<R> then(Function<? super T, R> fn) {
		return new FailedPromise<>(failure);
	}

	@Override
	public <R> Promise<R> bind(Function<? super T, Promise<R>> fn) {
		return new FailedPromise<>(failure);
	}

	@Override
	public <T2, R> Promise<R> zip(Promise<T2> other, BiFunction<? super T, ? super T2, R> fn) {
		return new FailedPromise<>(failure);
	}

	@Override
	public <R> R fold(Function<? super T, R> success, Function<? super Throwable, R> failure) {
		return failure.apply(this.failure);
	}

	@Override
	public Promise<T> recover(Function<? super Throwable, T> fn) {
		return new DonePromise<>(fn.apply(failure));
	}

	@SuppressWarnings("unchecked")
	@Override
	public <Err extends Throwable> Promise<T> recover(Class<? extends Err> expected, Function<? super Err, T> fn) {
		Exception cause = deep(failure);

		if (expected.isInstance(cause))
			return new DonePromise<>(fn.apply((Err) failure));
		else
			return this;
	}

	@Override
	public Promise<T> recover(Predicate<? super Throwable> p, Function<? super Throwable, T> fn) {
		Exception cause = deep(failure);

		if (cause != null && p.test(cause))
			return new DonePromise<>(fn.apply(failure));
		else
			return this;
	}

	@Override
	public <Err extends Throwable> Promise<T> failure(Function<? super Throwable, Err> fn) {
		Exception cause = deep(failure);

		if (cause != null)
			return new FailedPromise<>(fn.apply(cause));
		else
			return new FailedPromise<>(failure);
	}

	@Override
	public Promise<T> onFailure(Consumer<? super Throwable> fn) {
		fn.accept(failure);
		return this;
	}

	private Exception deep(Throwable t) {
		if (t instanceof CompletionException || t instanceof ExecutionException) return deep(t.getCause()); else return (Exception) t;
	}

	@Override
	public Attempt<T> join() {
		return Attempt.failed(failure);
	}

	@Override
	public CompletableFuture<T> future() {
		return CompletableFuture.failedFuture(failure);
	}

	@Override
	public Promise<T> subscribe(Subscriber<? super T, Throwable> subscriber) {
		subscriber.failure(failure);
		subscriber.done();
		return this;
	}

	@Override
	public Attempt<Void> dispose() {
		return Attempt.failed(failure);
	}
}
