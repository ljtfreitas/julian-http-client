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
import java.util.function.Supplier;

public interface Except<T> {

	T unsafe();

	Except<T> consumes(ThrowableConsumer<? super T> fn);

	<R> Except<R> map(ThrowableFunction<? super T, R> fn);

	Except<T> failure(Consumer<? super Throwable> fn);

	Except<T> recover(Class<? extends Throwable> candidate, ThrowableSupplier<T> fn);

	<E extends Throwable> Except<T> failure(Class<E> candidate, Function<E, ? extends Throwable> fn);

	T recover(Supplier<T> fn);

	<E extends Exception> T prop(Function<? super Throwable, E> fn) throws E;

	static <T> Except<T> run(ThrowableSupplier<T> fn) {
		try {
			return new Success<>(fn.get());
		} catch (Throwable t) {
			return new Failure<>(t);
		}
	}

	class Success<T> implements Except<T> {

		private final T value;

		private Success(T value) {
			this.value = value;
		}

		@Override
		public T unsafe() {
			return value;
		}

		@Override
		public Except<T> consumes(ThrowableConsumer<? super T> fn) {
			try {
				fn.accept(value);
				return this;
			} catch (Throwable t) {
				return new Failure<>(t);
			}
		}

		@Override
		public <R> Except<R> map(ThrowableFunction<? super T, R> fn) {
			try {
				return new Success<>(fn.apply(value));
			} catch (Throwable t) {
				return new Failure<>(t);
			}
		}

		@Override
		public Except<T> failure(Consumer<? super Throwable> fn) {
			return this;
		}
		
		@Override
		public Except<T> recover(Class<? extends Throwable> candidate, ThrowableSupplier<T> fn) {
			return this;
		}

		@Override
		public <E extends Throwable> Except<T> failure(Class<E> candidate, Function<E, ? extends Throwable> fn) {
			return this;
		}
		
		@Override
		public T recover(Supplier<T> fn) {
			return value;
		}

		@Override
		public <E extends Exception> T prop(Function<? super Throwable, E> fn) throws E {
			return value;
		}
	}

	class Failure<T> implements Except<T> {

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
		public Except<T> consumes(ThrowableConsumer<? super T> fn) {
			return this;
		}

		@Override
		public <R> Except<R> map(ThrowableFunction<? super T, R> fn) {
			return new Failure<>(value);
		}

		@Override
		public Except<T> failure(Consumer<? super Throwable> fn) {
			fn.accept(value);
			return this;
		}
		
		@Override
		public Except<T> recover(Class<? extends Throwable> candidate, ThrowableSupplier<T> fn) {
			return candidate.isInstance(value) ? Except.run(fn) : this;
		}

		@SuppressWarnings("unchecked")
		@Override
		public <E extends Throwable> Except<T> failure(Class<E> candidate, Function<E, ? extends Throwable> fn) {
			return candidate.isInstance(value) ? new Failure<>(fn.apply((E) value)) : this;
		}

		@Override
		public T recover(Supplier<T> fn) {
			return fn.get();
		}

		@Override
		public <E extends Exception> T prop(Function<? super Throwable, E> fn) throws E {
			throw fn.apply(value);
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
}
