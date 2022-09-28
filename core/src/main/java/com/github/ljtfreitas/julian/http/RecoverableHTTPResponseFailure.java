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
import com.github.ljtfreitas.julian.Kind;
import com.github.ljtfreitas.julian.http.client.HTTPClientResponse;
import com.github.ljtfreitas.julian.http.codec.HTTPResponseReaders;

public class RecoverableHTTPResponseFailure implements HTTPResponseFailure {

	private static final HTTPResponseFailure DEFAULT_FAILURE_STRATEGY = DefaultHTTPResponseFailure.get();

	private final HTTPResponseReaders readers;

	public RecoverableHTTPResponseFailure(HTTPResponseReaders readers) {
		this.readers = readers;
	}

	@Override
	public <T> RecoverableHTTPResponse<T> apply(HTTPClientResponse response, JavaType javaType) {
		FailureHTTPResponse<T> failed = DEFAULT_FAILURE_STRATEGY.<T> apply(response, javaType).cast(new Kind<FailureHTTPResponse<T>>() {})
				.orElseThrow(() -> new IllegalStateException("a FailureHTTPResponse is expected here..."));

		return new RecoverableHTTPResponse<>(failed, readers);
	}
}
