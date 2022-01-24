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

public interface Response<T, E extends Exception> {

	Except<T> body();

	<R> Response<R, E> map(Function<? super T, R> fn);

	Response<T, E> onFailure(Consumer<? super E> fn);

	Response<T, E> recover(Function<? super E, T> fn);

	Response<T, E> recover(Predicate<? super E> p, Function<? super E, T> fn);

	<Err extends E> Response<T, E> recover(Class<? extends Err> expected, Function<? super Err, T> fn);

	<R> R fold(Function<T, R> success, Function<? super E, R> failure);

	default Response<T, E> onSuccess(Consumer<? super T> fn) {
		body().onSuccess(fn::accept);
		return this;
	}

	@SuppressWarnings("unchecked")
	default <Err extends Exception, R extends Response<T, Err>> Optional<R> cast(Kind<R> candidate) {
		Class<?> javaType = candidate.javaType().rawClassType();
		return javaType.isAssignableFrom(this.getClass()) ? Optional.of((R) javaType.cast(this)) : Optional.empty();
	}

	static <T> Response<T, Exception> done(T value) {
		return new DoneResponse<>(value);
	}
}
