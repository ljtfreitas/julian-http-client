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

package com.github.ljtfreitas.julian.http;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import com.github.ljtfreitas.julian.Response;

public interface HTTPResponse<T> extends Response<T, HTTPResponseException> {

	HTTPStatus status();

	HTTPHeaders headers();

	@Override
	<R> HTTPResponse<R> map(Function<? super T, R> fn);

	<R> HTTPResponse<R> map(HTTPResponseFn<? super T, R> fn);

	@Override
	HTTPResponse<T> onSuccess(Consumer<? super T> fn);

	HTTPResponse<T> onSuccess(HTTPResponseConsumer<? super T> fn);

	@Override
	default HTTPResponse<T> onFailure(Consumer<? super HTTPResponseException> fn) {
		return this;
	}

	@Override
	default HTTPResponse<T> recover(Function<? super HTTPResponseException, T> fn) {
		return this;
	}

	@Override
	default HTTPResponse<T> recover(Predicate<? super HTTPResponseException> p, Function<? super HTTPResponseException, T> fn) {
		return this;
	}

	@Override
	default <Err extends HTTPResponseException> HTTPResponse<T> recover(Class<? extends Err> expected, Function<? super Err, T> fn) {
		return this;
	}

	default HTTPResponse<T> recover(HTTPStatusCode code, Function<HTTPResponse<T>, T> fn) {
		return this;
	}

	interface HTTPResponseFn<T, R> {

		R apply(HTTPStatus status, HTTPHeaders headers, T body);
	}

	interface HTTPResponseConsumer<T> {

		void accept(HTTPStatus status, HTTPHeaders headers, T body);
	}

	static <T> HTTPResponse<T> success(HTTPStatus status, HTTPHeaders headers, T body) {
		return new SuccessHTTPResponse<>(status, headers, body);
	}

	static <T> HTTPResponse<T> empty(HTTPStatus status, HTTPHeaders headers) {
		return new EmptyHTTPResponse<>(status, headers);
	}

	static <T> HTTPResponse<T> failed(HTTPStatus status, HTTPHeaders headers, HTTPResponseException failure) {
		return new FailureHTTPResponse<>(status, headers, failure);
	}
}
