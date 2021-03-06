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

import com.github.ljtfreitas.julian.JavaType;
import com.github.ljtfreitas.julian.Promise;
import com.github.ljtfreitas.julian.http.client.HTTPClientResponse;

import static java.util.function.Function.identity;

public class DefaultHTTPResponseFailure implements HTTPResponseFailure {

	private static final DefaultHTTPResponseFailure SINGLE_INSTANCE = new DefaultHTTPResponseFailure();

	@Override
	public <T> HTTPResponse<T> apply(HTTPClientResponse response, JavaType javaType) {
		HTTPStatus status = response.status();
		HTTPHeaders headers = response.headers();

		Promise<byte[]> bodyAsBytes = response.body().readAsBytes(identity()).map(Promise::pending).orElseGet(() -> Promise.done(new byte[0]));

		HTTPResponseException ex = HTTPStatusCode.select(status.code())
				.<HTTPResponseException>map(httpStatusCode -> HTTPFailureResponseException.create(httpStatusCode, headers, bodyAsBytes))
				.orElseGet(() -> unknown(status, headers, bodyAsBytes));

		return new FailureHTTPResponse<>(ex);
	}

	private HTTPUnknownFailureResponseException unknown(HTTPStatus status, HTTPHeaders headers, Promise<byte[]> responseBody) {
		return new HTTPUnknownFailureResponseException(status, headers, responseBody);
	}

	public static DefaultHTTPResponseFailure get() {
		return SINGLE_INSTANCE;
	}
}
