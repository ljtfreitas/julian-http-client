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
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static com.github.ljtfreitas.julian.Attempt.ThrowableFunction.identity;

public interface Bracket<T extends AutoCloseable> extends Attempt<T> {

	static <T extends AutoCloseable> Bracket<T> acquire(AutoCloseableSupplier<T> fn) {
		return new Acquired<>(fn);
	}
	
	<R extends AutoCloseable> Bracket<R> and(AutoCloseableFunction<? super T, R> fn);

	class Acquired<T extends AutoCloseable> implements Bracket<T> {

		private final AutoCloseableSupplier<T> supplier;

		private Acquired(AutoCloseableSupplier<T> supplier) {
			this.supplier = supplier;
		}

		private Attempt<T> id() {
			return map(identity());
		}

		@Override
		public T unsafe() {
			return id().unsafe();
		}

		@Override
		public <R> Attempt<R> map(Attempt.ThrowableFunction<? super T, R> fn) {
			return Attempt.run(() -> safe(fn));
		}

		private <R> R safe(Attempt.ThrowableFunction<? super T, R> fn) throws Throwable {
			try (T value = supplier.get()) {
				return fn.apply(value);
			}
		}

		@Override
		public <R> Attempt<R> bind(Function<? super T, Attempt<R>> fn) {
			return id().bind(fn);
		}

		@Override
		public <E extends Throwable> T prop() throws E {
			return id().prop();
		}

		@Override
		public <E extends Throwable> T prop(Function<? super Throwable, E> fn) throws E {
			return id().prop(fn);
		}

		@Override
		public Attempt<T> recover(ThrowableFunction<? super Throwable, T> fn) {
			return id().recover(fn);
		}

		@Override
		public Attempt<T> recover(Class<? extends Throwable> candidate, Attempt.ThrowableSupplier<T> fn) {
			return id().recover(candidate, fn);
		}

		@Override
		public T recover(Supplier<T> fn) {
			return id().recover(fn);
		}

		@Override
		public Attempt<T> recover(Predicate<? super Throwable> candidate, ThrowableSupplier<T> fn) {
			return id().recover(candidate, fn);
		}

		@Override
		public Attempt<T> failure(Function<? super Throwable, ? extends Throwable> fn) {
			return id().failure(fn);
		}

		@Override
		public <E extends Throwable> Attempt<T> failure(Class<E> candidate, Function<? super E, ? extends Throwable> fn) {
			return id().failure(candidate, fn);
		}

		@Override
		public Attempt<T> failure(Predicate<? super Throwable> candidate, Function<? super Throwable, ? extends Throwable> fn) {
			return id().failure(candidate, fn);
		}

		@Override
		public Attempt<T> onSuccess(Attempt.ThrowableConsumer<? super T> fn) {
			return id().onSuccess(fn);
		}

		@Override
		public Attempt<T> onFailure(Consumer<? super Throwable> fn) {
			return id().onFailure(fn);
		}
		
		@Override
		public <R extends AutoCloseable> Bracket<R> and(AutoCloseableFunction<? super T, R> fn) {
			return new Acquired<>(() -> safe(fn::apply));
		}

		@Override
		public <R> R fold(Function<? super T, R> success, Function<? super Throwable, R> failure) {
			return id().fold(success, failure);
		}

		@Override
		public Optional<T> op() {
			return id().op();
		}

		@Override
		public Promise<T> promise() {
			return id().promise();
		}
	}

	interface AutoCloseableSupplier<T> {

		T get() throws Throwable;
	}

	interface AutoCloseableFunction<T, R> {

		R apply(T t) throws Throwable;
	}
}
