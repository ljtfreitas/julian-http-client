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

import com.github.ljtfreitas.julian.Attempt;
import com.github.ljtfreitas.julian.Promise;
import com.github.ljtfreitas.julian.Subscriber;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import static com.github.ljtfreitas.julian.Preconditions.nonNull;

public class FailureHTTPResponse<T> implements HTTPResponse<T> {

	private final HTTPResponseException failure;
	private final HTTPStatus status;
	private final HTTPHeaders headers;

	public FailureHTTPResponse(HTTPResponseException failure) {
		this.failure = nonNull(failure);
		this.status = failure.status();
		this.headers = failure.headers();
	}

	public HTTPResponseException asException() {
		return failure;
	}

	@Override
	public Attempt<T> body() {
		return Attempt.failed(failure);
	}

	@Override
	public <R> FailureHTTPResponse<R> map(Function<? super T, R> fn) {
		return new FailureHTTPResponse<>(failure);
	}

	@Override
	public <R> FailureHTTPResponse<R> map(HTTPResponseFn<? super T, R> fn) {
		return new FailureHTTPResponse<>(failure);
	}

	@Override
	public FailureHTTPResponse<T> onSuccess(Consumer<? super T> fn) {
		return this;
	}

	@Override
	public FailureHTTPResponse<T> onSuccess(HTTPResponseConsumer<? super T> fn) {
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
	public FailureHTTPResponse<T> onFailure(Consumer<? super HTTPResponseException> fn) {
		fn.accept(failure);
		return this;
	}

	@Override
	public HTTPResponse<T> recover(Function<? super HTTPResponseException, T> fn) {
		return new LazyHTTPResponse<>(status, headers, Attempt.run(() -> fn.apply(failure)).promise());
	}

	@Override
	public HTTPResponse<T> recover(Class<? extends HTTPResponseException> expected, Function<? super HTTPResponseException, T> fn) {
		return expected.isInstance(failure) ? new LazyHTTPResponse<>(status, headers, Attempt.run(() -> fn.apply(failure)).promise()) : this;
	}

	@Override
	public HTTPResponse<T> recover(Predicate<? super HTTPResponseException> p, Function<? super HTTPResponseException, T> fn) {
		return p.test(failure) ? new LazyHTTPResponse<>(status, headers, Attempt.run(() -> fn.apply(failure)).promise()) : this;
	}

	@Override
	public HTTPResponse<T> recover(HTTPStatusCode code, HTTPResponseFn<byte[], T> fn) {
		return status.is(code) ? new LazyHTTPResponse<>(status, headers, Attempt.run(() -> fn.apply(status, headers, failure.bodyAsBytes())).promise()) : this;
	}

	@Override
	public <R> R fold(Function<? super T, R> success, Function<? super HTTPResponseException, R> failure) {
		return failure.apply(this.failure);
	}

	@Override
	public HTTPResponse<T> subscribe(Subscriber<? super T, HTTPResponseException> subscriber) {
		subscriber.failure(failure);
		subscriber.done();
		return this;
	}

	@Override
	public HTTPResponse<T> subscribe(HTTPResponseSubscriber<? super T> subscriber) {
		subscriber.failure(failure);
		subscriber.done();
		return this;
	}

	@Override
	public Promise<T> promise() {
		return Promise.failed(failure);
	}

	@Override
	public String toString() {
		return status + "\n" + headers;
	}
}
