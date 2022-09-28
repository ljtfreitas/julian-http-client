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

package com.github.ljtfreitas.julian.http.client;

import com.github.ljtfreitas.julian.Response;
import com.github.ljtfreitas.julian.http.HTTPHeader;
import com.github.ljtfreitas.julian.http.HTTPHeaders;
import com.github.ljtfreitas.julian.http.HTTPResponseBody;
import com.github.ljtfreitas.julian.http.HTTPStatus;
import com.github.ljtfreitas.julian.http.HTTPStatusCode;

import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Flow.Publisher;
import java.util.function.Function;

class DefaultHTTPClientResponse implements HTTPClientResponse {

	private final HTTPStatus status;
	private final HTTPHeaders headers;
	private final HTTPResponseBody body;

	DefaultHTTPClientResponse(HTTPStatus status, HTTPHeaders headers, HTTPResponseBody body) {
		this.status = status;
		this.headers = headers;
		this.body = body;
	}

	@Override
	public HTTPStatus status() {
		return status;
	}

	@Override
	public HTTPResponseBody body() {
		return body;
	}

	@Override
	public HTTPHeaders headers() {
		return headers;
	}

	@Override
	public <T, R extends Response<T, ? extends Throwable>> Optional<R> success(Function<? super HTTPClientResponse, R> fn) {
		return status.isSuccess() || status.isRedirection() ? Optional.ofNullable(fn.apply(this)) : Optional.empty();
	}

	@Override
	public <T, R extends Response<T, ? extends Throwable>> Optional<R> failure(Function<? super HTTPClientResponse, R> fn) {
		return status.isError() ? Optional.ofNullable(fn.apply(this)) : Optional.empty();
	}

	static DefaultHTTPClientResponse valueOf(HttpResponse<Publisher<List<ByteBuffer>>> response) {
		HTTPStatus status = HTTPStatusCode.select(response.statusCode()).map(HTTPStatus::new)
				.orElseGet(() -> HTTPStatus.valueOf(response.statusCode()));

		HTTPHeaders headers = response.headers().map().entrySet().stream()
				.map(e -> HTTPHeader.create(e.getKey(), e.getValue()))
				.reduce(HTTPHeaders.empty(), HTTPHeaders::join, (a, b) -> b);

		HTTPResponseBody body = HTTPResponseBody.optional(status, headers, () -> HTTPResponseBody.lazy(response.body()));

		return new DefaultHTTPClientResponse(status, headers, body);
	}
}
