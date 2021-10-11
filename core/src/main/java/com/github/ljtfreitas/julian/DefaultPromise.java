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
import java.util.function.Function;

import static com.github.ljtfreitas.julian.Except.run;

class DefaultPromise<T> implements Promise<T> {

	private final CompletableFuture<T> future;

	DefaultPromise(CompletableFuture<T> future) {
		this.future = future;
	}

	@Override
	public <R> Promise<R> then(Function<? super T, R> fn) {
		return new DefaultPromise<>(future.thenApply(fn));
	}

	@Override
	public <R> Promise<R> bind(Function<? super T, Promise<R>> fn) {
		return new DefaultPromise<>(future.thenCompose(t -> fn.apply(t).future()));
	}

	@Override
	public Promise<T> failure(Function<Throwable, ? extends Exception> fn) {
		return new DefaultPromise<>(future.handle((r, e) -> { if (e == null) return r; else throw failure(fn.apply(e));}));
	}
	
	private RuntimeException failure(Exception e) {
		return (e instanceof RuntimeException) ? (RuntimeException) e : new CompletionException(e);
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

	static <T> Promise<T> done(T value) {
		return new DefaultPromise<>(CompletableFuture.completedFuture(value));
	}

	static <T> Promise<T> failed(Throwable t) {
		return new DefaultPromise<>(CompletableFuture.failedFuture(t));
	}
}
