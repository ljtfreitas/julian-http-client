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
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

class DefaultPromise<T, E extends Exception> implements Promise<T, E> {

	private final CompletableFuture<T> future;

	DefaultPromise(CompletableFuture<T> future) {
		this.future = future;
	}

	@Override
	public Promise<T, E> onSuccess(Consumer<T> fn) {
		return new DefaultPromise<>(future.whenCompleteAsync((r, e) -> { if (e == null) fn.accept(r); }));
	}

	@Override
	public <R> Promise<R, E> then(Function<? super T, R> fn) {
		return new DefaultPromise<>(future.thenApplyAsync(fn));
	}

	@Override
	public <R, Err extends Exception> Promise<R, Err> bind(Function<? super T, Promise<R, Err>> fn) {
		return new DefaultPromise<>(future.thenComposeAsync(t -> fn.apply(t).future()));
	}

	@SuppressWarnings("unchecked")
	@Override
	public Promise<T, E> recover(Function<? super E, T> fn) {
		return new DefaultPromise<>(future.exceptionally(t -> fn.apply((E) t)));
	}

	@Override
	public <Err extends E> Promise<T, E> recover(Class<? extends Err> expected, Function<? super Err, T> fn) {
		return new DefaultPromise<>(future.handleAsync((r, e) -> {
			Exception cause = deep(e);
			if (expected.isInstance(cause))
				return fn.apply(expected.cast(cause));
			else
				return r;
		}));
	}

	@SuppressWarnings("unchecked")
	@Override
	public Promise<T, E> recover(Predicate<? super E> p, Function<? super E, T> fn) {
		return new DefaultPromise<>(future.handleAsync((r, e) -> {
			Exception cause = deep(e);
			if (cause != null && p.test((E) cause))
				return fn.apply((E) cause);
			else
				return r;
		}));
	}

	@SuppressWarnings("unchecked")
	@Override
	public <Err extends Exception> Promise<T, Err> failure(Function<? super E, Err> fn) {
		return new DefaultPromise<>(future.handleAsync((r, e) -> {
			Exception cause = deep(e);
			if (cause != null)
				throw failure(fn.apply((E) cause));
			else
				return r;
		}));
	}

	@SuppressWarnings("unchecked")
	@Override
	public Promise<T, E> onFailure(Consumer<? super E> fn) {
		return new DefaultPromise<>(future.whenCompleteAsync((r, e) -> {
			Exception cause = deep(e);
			if (cause != null)
				fn.accept((E) cause);
		}));
	}

	private RuntimeException failure(Exception e) {
		return (e instanceof RuntimeException) ? (RuntimeException) e : new CompletionException(e);
	}

	private Exception deep(Throwable t) {
		if (t instanceof CompletionException || t instanceof ExecutionException) return deep(t.getCause()); else return (Exception) t;
	}

	@Override
	public Except<T> join() {
		return Except.run(future::join)
				.failure(CompletionException.class, Exception::getCause)
				.failure(ExecutionException.class, Exception::getCause);
	}

	@Override
	public CompletableFuture<T> future() {
		return future;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Promise<T, E> subscribe(Subscriber<? super T, ? super E> subscriber) {
		BiConsumer<T, Throwable> handle = (r, e) -> { if (e == null) subscriber.success(r); else subscriber.failure((E) e); };
		BiConsumer<T, Throwable> done = (r, e) -> subscriber.done();
		return new DefaultPromise<>(future.whenCompleteAsync(handle).whenCompleteAsync(done));
	}
}
