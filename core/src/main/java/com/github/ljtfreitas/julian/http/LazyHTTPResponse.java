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

class LazyHTTPResponse<T> implements HTTPResponse<T> {

	private final HTTPStatus status;
	private final HTTPHeaders headers;
	private final Promise<T> body;

	LazyHTTPResponse(HTTPStatus status, HTTPHeaders headers, Promise<T> body) {
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
		return body.join();
	}

	@Override
	public <R> HTTPResponse<R> map(Function<? super T, R> fn) {
		return new LazyHTTPResponse<>(status, headers, body.then(fn));
	}

	@Override
	public <R> HTTPResponse<R> map(HTTPResponseFn<? super T, R> fn) {
		return new LazyHTTPResponse<>(status, headers, body.then(content -> fn.apply(status, headers, content)));
	}

	@Override
	public <R> R fold(Function<? super T, R> success, Function<? super Exception, R> failure) {
		return body.fold(success, failure);
	}

	@Override
	public HTTPResponse<T> subscribe(Subscriber<? super T> subscriber) {
		return new LazyHTTPResponse<>(status, headers, body.subscribe(subscriber));
	}

	@Override
	public HTTPResponse<T> subscribe(HTTPResponseSubscriber<? super T> subscriber) {
		return new LazyHTTPResponse<>(status, headers, body.subscribe(new Subscriber<>() {

			@Override
			public void success(T value) {
				subscriber.success(status, headers, value);
			}

			@Override
			public void failure(Exception failure) {
				subscriber.failure(failure);
			}

			@Override
			public void done() {
				subscriber.done();
			}
		}));
	}

	@Override
	public HTTPResponse<T> onSuccess(Consumer<? super T> fn) {
		body.onSuccess(fn);
		return this;
	}

	@Override
	public HTTPResponse<T> onSuccess(HTTPResponseConsumer<? super T> fn) {
		body.onSuccess(content ->fn.accept(status, headers, content));
		return this;
	}

	@Override
	public HTTPResponse<T> onFailure(Consumer<? super Exception> fn) {
		return new LazyHTTPResponse<>(status, headers, body.onFailure(fn));
	}

	@Override
	public HTTPResponse<T> recover(Function<? super Exception, T> fn) {
		return new LazyHTTPResponse<>(status, headers, body.recover(fn));
	}

	@Override
	public HTTPResponse<T> recover(Predicate<? super Exception> p, Function<? super Exception, T> fn) {
		return new LazyHTTPResponse<>(status, headers, body.recover(p, fn));
	}

	@Override
	public <Err extends Exception> HTTPResponse<T> recover(Class<? extends Err> expected, Function<? super Err, T> fn) {
		return new LazyHTTPResponse<>(status, headers, body.recover(expected, fn));
	}

	@Override
	public HTTPResponse<T> recover(HTTPStatusCode code, HTTPResponseFn<byte[], T> fn) {
		return (status.is(code)) ?
				new LazyHTTPResponse<>(status, headers, body.recover(HTTPResponseException.class, e -> fn.apply(status, headers, e.bodyAsBytes()))) :
				this;
	}
}
