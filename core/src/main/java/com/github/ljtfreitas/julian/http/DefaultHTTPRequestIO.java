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

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

import com.github.ljtfreitas.julian.Promise;
import com.github.ljtfreitas.julian.RequestIO;
import com.github.ljtfreitas.julian.http.client.HTTPClient;
import com.github.ljtfreitas.julian.http.client.HTTPClientException;
import com.github.ljtfreitas.julian.http.client.HTTPClientResponse;
import com.github.ljtfreitas.julian.http.codec.ContentType;
import com.github.ljtfreitas.julian.http.codec.HTTPMessageException;
import com.github.ljtfreitas.julian.http.codec.HTTPResponseReader;
import com.github.ljtfreitas.julian.http.codec.HTTPResponseReaderException;
import com.github.ljtfreitas.julian.http.codec.HTTPResponseReaders;

import static com.github.ljtfreitas.julian.Message.format;
import static com.github.ljtfreitas.julian.http.HTTPHeader.CONTENT_TYPE;

class DefaultHTTPRequestIO<T> implements RequestIO<T> {

	private final HTTPRequest<T> source;
	private final HTTPClient httpClient;
	private final HTTPResponseReaders readers;
	private final HTTPResponseFailure failure;
	
	DefaultHTTPRequestIO(HTTPRequest<T> source, HTTPClient httpClient, HTTPResponseReaders readers, HTTPResponseFailure failure) {
		this.source = source;
		this.httpClient = httpClient;
		this.readers = readers;
		this.failure = failure;
	}

	@Override
	public Promise<HTTPResponse<T>> execute() {
		return httpClient.request(source).execute()
				.then(this::read)
				.failure(e -> exceptionally(deep(e)));
	}

	private HTTPException exceptionally(Throwable e) {
		if (e instanceof HTTPClientException) {
			return (HTTPClientException) e;

		} else if (e instanceof HTTPMessageException) {
			return (HTTPMessageException) e;

		} else if (e instanceof IOException) {
			return new HTTPClientException("I/O error: [" + source.method() + " " + source.path() + "]", e);

		} else {
			return new HTTPException("Error on HTTP request: [" + source.method() + " " + source.path() + "]", e);
		}
	}

	private Throwable deep(Throwable e) {
		return (e instanceof CompletionException || e instanceof ExecutionException) ? deep(e.getCause()) : e;
	}

	private HTTPResponse<T> read(HTTPClientResponse response) {
		return response.failure(this::failure)
				.orElseGet(() -> response.success(this::success)
						.orElseGet(() -> empty(response)));
	}

	private HTTPResponse<T> empty(HTTPClientResponse response) {
		return new EmptyHTTPResponse<>(response.status(), response.headers());
	}

	private HTTPResponse<T> failure(HTTPClientResponse response) {
		return failure.apply(response, source.returnType());
	}

	private HTTPResponse<T> success(HTTPClientResponse response) {
		Optional<HTTPResponse<T>> r = response.body()
				.available(b -> deserialize(b, response.headers()))
				.map(b -> new DefaultHTTPResponse<>(response.status(), response.headers(), b));

		return r.orElseGet(() -> empty(response));
	}

	@SuppressWarnings("unchecked")
	private T deserialize(HTTPResponseBody body, HTTPHeaders headers) {
		ContentType contentType = headers.select(CONTENT_TYPE)
				.map(h -> ContentType.valueOf(h.value()))
				.orElseGet(ContentType::wildcard);

		HTTPResponseReader<?> reader = readers.select(contentType, source.returnType())
				.orElseThrow(() -> new HTTPResponseReaderException(format("There is not any HTTPResponseReader able to convert {0} to {1}", contentType, source.returnType())));

		return (T) body.deserialize(reader, source.returnType());
	}
}
