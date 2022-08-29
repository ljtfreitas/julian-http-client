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
	public <R> HTTPResponse<R> map(Function<? super T, R> fn) {
		return new FailureHTTPResponse<>(failure);
	}

	@Override
	public <R> HTTPResponse<R> map(HTTPResponseFn<? super T, R> fn) {
		return new FailureHTTPResponse<>(failure);
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
	public HTTPResponse<T> onFailure(Consumer<? super Exception> fn) {
		fn.accept(failure);
		return this;
	}

	@Override
	public HTTPResponse<T> recover(Function<? super Exception, T> fn) {
		return new SuccessHTTPResponse<>(status, headers, fn.apply(failure));
	}

	@Override
	public <Err extends Exception> HTTPResponse<T> recover(Class<? extends Err> expected, Function<? super Err, T> fn) {
		return expected.isInstance(failure) ? new SuccessHTTPResponse<>(status, headers, fn.apply(expected.cast(failure))) : this;
	}

	@Override
	public HTTPResponse<T> recover(Predicate<? super Exception> p, Function<? super Exception, T> fn) {
		return p.test(failure) ? new SuccessHTTPResponse<>(status, headers, fn.apply(failure)) : this;
	}

	@Override
	public HTTPResponse<T> recover(HTTPStatusCode code, HTTPResponseFn<byte[], T> fn) {
		return status.is(code) ? new SuccessHTTPResponse<>(status, headers, fn.apply(status, headers, failure.bodyAsBytes())) : this;
	}

	@Override
	public <R> R fold(Function<? super T, R> success, Function<? super Exception, R> failure) {
		return failure.apply(this.failure);
	}

	@Override
	public HTTPResponse<T> subscribe(Subscriber<? super T> subscriber) {
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
}
