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

class SuccessHTTPResponse<T> implements HTTPResponse<T> {

	private final HTTPStatus status;
	private final HTTPHeaders headers;
	private final T body;

	SuccessHTTPResponse(HTTPStatus status, HTTPHeaders headers, T body) {
		this.status = status;
		this.headers = headers;
		this.body = body;
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
	public Attempt<T> body() {
		return Attempt.success(body);
	}

	@Override
	public <R> HTTPResponse<R> map(Function<? super T, R> fn) {
		return new SuccessHTTPResponse<>(status, headers, fn.apply(body));
	}

	@Override
	public <R> HTTPResponse<R> map(HTTPResponseFn<? super T, R> fn) {
		return new SuccessHTTPResponse<>(status, headers, fn.apply(status, headers, body));
	}

	@Override
	public <R> R fold(Function<? super T, R> success, Function<? super Throwable, R> failure) {
		return success.apply(body);
	}

	@Override
	public HTTPResponse<T> subscribe(HTTPResponseSubscriber<? super T> subscriber) {
		subscriber.success(status, headers, body);
		subscriber.done();
		return this;
	}

	@Override
	public HTTPResponse<T> subscribe(Subscriber<? super T> subscriber) {
		subscriber.success(body);
		subscriber.done();
		return this;
	}

	@Override
	public HTTPResponse<T> onSuccess(Consumer<? super T> fn) {
		fn.accept(body);
		return this;
	}

	@Override
	public HTTPResponse<T> onSuccess(HTTPResponseConsumer<? super T> fn) {
		fn.accept(status, headers, body);
		return this;
	}

	@Override
	public HTTPResponse<T> onFailure(Consumer<? super Throwable> fn) {
		return this;
	}

	@Override
	public HTTPResponse<T> recover(Function<? super Throwable, T> fn) {
		return this;
	}

	@Override
	public HTTPResponse<T> recover(Predicate<? super Throwable> p, Function<? super Throwable, T> fn) {
		return this;
	}

	@Override
	public <Err extends Throwable> HTTPResponse<T> recover(Class<? extends Err> expected, Function<? super Err, T> fn) {
		return this;
	}

	@Override
	public HTTPResponse<T> recover(HTTPStatusCode code, HTTPResponseFn<byte[], T> fn) {
		return this;
	}
}
