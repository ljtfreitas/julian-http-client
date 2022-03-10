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
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

class DonePromise<T> implements Promise<T> {

	private final T value;

	DonePromise(T value) {
		this.value = value;
	}

	@Override
	public Promise<T> onSuccess(Consumer<? super T> fn) {
		fn.accept(value);
		return this;
	}

	@Override
	public <R> Promise<R> then(Function<? super T, R> fn) {
		return new DonePromise<>(fn.apply(value));
	}

	@Override
	public <R> R fold(Function<? super T, R> success, Function<? super Exception, R> failure) {
		return success.apply(value);
	}

	@Override
	public <R> Promise<R> bind(Function<? super T, Promise<R>> fn) {
		return fn.apply(value);
	}

	@Override
	public <T2, R> Promise<R> zip(Promise<T2> other, BiFunction<? super T, ? super T2, R> fn) {
		return other.then(t2 -> fn.apply(value, t2));
	}

	@Override
	public Promise<T> recover(Function<? super Exception, T> fn) {
		return this;
	}

	@Override
	public <Err extends Exception> Promise<T> recover(Class<? extends Err> expected, Function<? super Err, T> fn) {
		return this;
	}

	@Override
	public Promise<T> recover(Predicate<? super Exception> p, Function<? super Exception, T> fn) {
		return this;
	}

	@Override
	public <Err extends Exception> Promise<T> failure(Function<? super Exception, Err> fn) {
		return new DonePromise<>(value);
	}

	@Override
	public Promise<T> onFailure(Consumer<? super Exception> fn) {
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
	public Promise<T> subscribe(Subscriber<? super T> subscriber) {
		subscriber.success(value);
		subscriber.done();
		return this;
	}
}
