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

import com.github.ljtfreitas.julian.Except;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public class FailureHTTPResponse<T> implements HTTPResponse<T> {

	private final HTTPStatus status;
	private final HTTPHeaders headers;
	private final HTTPResponseException exception;

	public FailureHTTPResponse(HTTPStatus status, HTTPHeaders headers, HTTPResponseException exception) {
		this.status = status;
		this.headers = headers;
		this.exception = exception;
	}

	@Override
	public Except<T> body() {
		return Except.failed(exception);
	}

	@Override
	public <R> HTTPResponse<R> map(Function<? super T, R> fn) {
		return new FailureHTTPResponse<>(status, headers, exception);
	}

	@Override
	public <R> HTTPResponse<R> map(HTTPResponseFn<? super T, R> fn) {
		return new FailureHTTPResponse<>(status, headers, exception);
	}

	@Override
	public HTTPResponse<T> onSuccess(Consumer<? super T> fn) {
		return this;
	}

	@Override
	public HTTPResponse<T> onSuccess(HTTPResponseConsumer<? super T> fn) {
		return this;
	}

	@Override
	public HTTPStatus status() {
		return status;
	}

	@Override
	public HTTPHeaders headers() {
		return headers;
	}

	@Override
	public HTTPResponse<T> onFailure(Consumer<? super HTTPResponseException> fn) {
		fn.accept(exception);
		return this;
	}

	@Override
	public HTTPResponse<T> recover(Function<? super HTTPResponseException, T> fn) {
		return new SuccessHTTPResponse<>(status, headers, fn.apply(exception));
	}

	@Override
	public <Err extends HTTPResponseException> HTTPResponse<T> recover(Class<? extends Err> expected, Function<? super Err, T> fn) {
		return expected.isInstance(exception) ? new SuccessHTTPResponse<>(status, headers, fn.apply(expected.cast(exception))) : this;
	}

	@Override
	public HTTPResponse<T> recover(Predicate<? super HTTPResponseException> p, Function<? super HTTPResponseException, T> fn) {
		return p.test(exception) ? new SuccessHTTPResponse<>(status, headers, fn.apply(exception)) : this;
	}

	@Override
	public HTTPResponse<T> recover(HTTPStatusCode code, Function<HTTPResponse<T>, T> fn) {
		return status.is(code) ? new SuccessHTTPResponse<>(status, headers, fn.apply(this)) : this;
	}

	@Override
	public <R> R fold(Function<T, R> success, Function<? super HTTPResponseException, R> failure) {
		return failure.apply(exception);
	}
}
