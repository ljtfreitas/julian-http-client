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

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public interface Attempt<T> {

	T unsafe();

	Attempt<T> onSuccess(ThrowableConsumer<? super T> fn);

	<R> Attempt<R> map(ThrowableFunction<? super T, R> fn);

	<R> Attempt<R> bind(Function<? super T, Attempt<R>> fn);

	Attempt<T> onFailure(Consumer<? super Throwable> fn);

	Attempt<T> failure(Function<? super Throwable, ? extends Throwable> fn);

	<E extends Throwable> Attempt<T> failure(Class<E> candidate, Function<? super E, ? extends Throwable> fn);

	Attempt<T> failure(Predicate<? super Throwable> candidate, Function<? super Throwable, ? extends Throwable> fn);

	Attempt<T> recover(ThrowableFunction<? super Throwable, T> fn);

	Attempt<T> recover(Class<? extends Throwable> candidate, ThrowableSupplier<T> fn);

	Attempt<T> recover(Predicate<? super Throwable> candidate, ThrowableSupplier<T> fn);

	T recover(Supplier<T> fn);

	<E extends Exception> T prop() throws E;

	<E extends Exception> T prop(Function<? super Throwable, E> fn) throws E;

	<R> R fold(Function<? super T, R> success, Function<? super Throwable, R> failure);

	static <T> Attempt<T> run(ThrowableSupplier<T> fn) {
		try {
			return new Success<>(fn.get());
		} catch (Throwable t) {
			return new Failure<>(t);
		}
	}

	static Attempt<Void> just(ThrowableRunnable runnable) {
		try {
			runnable.run();
			return new Success<>(null);
		} catch (Throwable t) {
			return new Failure<>(t);
		}
	}

	static <T> Attempt<T> success(T value) {
		return new Success<>(value);
	}

	static <T> Attempt<T> failed(Throwable t) {
		return new Failure<>(t);
	}

	class Success<T> implements Attempt<T> {

		private final T value;

		private Success(T value) {
			this.value = value;
		}

		@Override
		public T unsafe() {
			return value;
		}

		@Override
		public Attempt<T> onSuccess(ThrowableConsumer<? super T> fn) {
			try {
				fn.accept(value);
				return this;
			} catch (Throwable t) {
				return new Failure<>(t);
			}
		}

		@Override
		public <R> Attempt<R> map(ThrowableFunction<? super T, R> fn) {
			try {
				return new Success<>(fn.apply(value));
			} catch (Throwable t) {
				return new Failure<>(t);
			}
		}

		@Override
		public <R> Attempt<R> bind(Function<? super T, Attempt<R>> fn) {
			return fn.apply(value);
		}

		@Override
		public Attempt<T> onFailure(Consumer<? super Throwable> fn) {
			return this;
		}

		@Override
		public Attempt<T> failure(Function<? super Throwable, ? extends Throwable> fn) {
			return this;
		}

		@Override
		public <E extends Throwable> Attempt<T> failure(Class<E> candidate, Function<? super E, ? extends Throwable> fn) {
			return this;
		}

		@Override
		public Attempt<T> failure(Predicate<? super Throwable> candidate, Function<? super Throwable, ? extends Throwable> fn) {
			return this;
		}

		@Override
		public Attempt<T> recover(Class<? extends Throwable> candidate, ThrowableSupplier<T> fn) {
			return this;
		}

		@Override
		public Attempt<T> recover(ThrowableFunction<? super Throwable, T> fn) {
			return this;
		}

		@Override
		public Attempt<T> recover(Predicate<? super Throwable> candidate, ThrowableSupplier<T> fn) {
			return this;
		}

		@Override
		public T recover(Supplier<T> fn) {
			return value;
		}

		@Override
		public <E extends Exception> T prop() throws E {
			return value;
		}

		@Override
		public <E extends Exception> T prop(Function<? super Throwable, E> fn) throws E {
			return value;
		}

		@Override
		public <R> R fold(Function<? super T, R> success, Function<? super Throwable, R> failure) {
			return success.apply(value);
		}
	}

	class Failure<T> implements Attempt<T> {

		private final Throwable value;

		private Failure(Throwable value) {
			this.value = value;
		}

		@Override
		public T unsafe() {
			if (value instanceof RuntimeException) {
				throw (RuntimeException) value;
			} else {
				throw new FailureException(value);
			}
		}

		@Override
		public Attempt<T> onSuccess(ThrowableConsumer<? super T> fn) {
			return this;
		}

		@Override
		public <R> Attempt<R> map(ThrowableFunction<? super T, R> fn) {
			return new Failure<>(value);
		}

		@Override
		public <R> Attempt<R> bind(Function<? super T, Attempt<R>> fn) {
			return new Failure<>(value);
		}

		@Override
		public Attempt<T> onFailure(Consumer<? super Throwable> fn) {
			fn.accept(value);
			return this;
		}

		@Override
		public T recover(Supplier<T> fn) {
			return fn.get();
		}

		@Override
		public Attempt<T> recover(ThrowableFunction<? super Throwable, T> fn) {
			return Attempt.run(() -> fn.apply(value));
		}

		@Override
		public Attempt<T> recover(Class<? extends Throwable> candidate, ThrowableSupplier<T> fn) {
			return candidate.isInstance(value) ? Attempt.run(fn) : this;
		}

		@Override
		public Attempt<T> recover(Predicate<? super Throwable> candidate, ThrowableSupplier<T> fn) {
			return candidate.test(value) ? Attempt.run(fn) : this;
		}

		@Override
		public Attempt<T> failure(Function<? super Throwable, ? extends Throwable> fn) {
			return new Failure<>(fn.apply(value));
		}

		@SuppressWarnings("unchecked")
		@Override
		public <E extends Throwable> Attempt<T> failure(Class<E> candidate, Function<? super E, ? extends Throwable> fn) {
			return candidate.isInstance(value) ? new Failure<>(fn.apply((E) value)) : this;
		}

		@Override
		public Attempt<T> failure(Predicate<? super Throwable> candidate, Function<? super Throwable, ? extends Throwable> fn) {
			return candidate.test(value) ? new Failure<>(fn.apply(value)) : this;
		}

		@Override
		public <E extends Exception> T prop() throws E {
			if (value instanceof RuntimeException) {
				throw (RuntimeException) value;
			} else {
				throw new FailureException(value);
			}
		}

		@Override
		public <E extends Exception> T prop(Function<? super Throwable, E> fn) throws E {
			throw fn.apply(value);
		}

		@Override
		public <R> R fold(Function<? super T, R> success, Function<? super Throwable, R> failure) {
			return failure.apply(value);
		}
	}

	class FailureException extends RuntimeException {

		private static final long serialVersionUID = 1L;

		private FailureException(Throwable cause) {
			super(cause);
		}
	}

	interface ThrowableSupplier<T> {

		T get() throws Throwable;
	}

	interface ThrowableConsumer<T> {

		void accept(T t) throws Throwable;
	}

	interface ThrowableFunction<T, R> {

		R apply(T t) throws Throwable;

		static <T> ThrowableFunction<T, T> identity() {
			return t -> t;
		}
	}

	interface ThrowableRunnable {

		void run() throws Throwable;
	}
}
