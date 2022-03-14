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

import com.github.ljtfreitas.julian.Promise;
import com.github.ljtfreitas.julian.http.HTTPEndpoint.Body;
import com.github.ljtfreitas.julian.http.client.HTTPClient;
import com.github.ljtfreitas.julian.http.codec.HTTPMessageCodecs;
import com.github.ljtfreitas.julian.http.codec.HTTPRequestWriterException;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executor;

import static com.github.ljtfreitas.julian.Message.format;
import static com.github.ljtfreitas.julian.http.HTTPHeader.CONTENT_TYPE;

public class DefaultHTTP implements HTTP {

	private final HTTPClient httpClient;
	private final HTTPRequestInterceptor interceptor;
	private final HTTPMessageCodecs codecs;
	private final HTTPResponseFailure failure;
	private final Charset encoding;
	private final Executor executor;

	public DefaultHTTP(HTTPClient httpClient, HTTPMessageCodecs codecs) {
		this(httpClient, codecs, HTTPRequestInterceptor.none());
	}

	public DefaultHTTP(HTTPClient httpClient, HTTPMessageCodecs codecs, HTTPRequestInterceptor interceptor) {
		this(httpClient, codecs, interceptor, new DefaultHTTPResponseFailure());
	}

	public DefaultHTTP(HTTPClient httpClient, HTTPMessageCodecs codecs, HTTPRequestInterceptor interceptor, HTTPResponseFailure failure) {
		this(httpClient, interceptor, codecs, failure, StandardCharsets.UTF_8, null);
	}

	public DefaultHTTP(HTTPClient httpClient, HTTPRequestInterceptor interceptor, HTTPMessageCodecs codecs, HTTPResponseFailure failure, Charset encoding, Executor executor) {
		this.httpClient = httpClient;
		this.interceptor = interceptor;
		this.codecs = codecs;
		this.failure = failure;
		this.encoding = encoding;
		this.executor = executor;
	}

	@Override
	public <T> Promise<HTTPResponse<T>> run(HTTPEndpoint endpoint) {
		HTTPRequestBody body = endpoint.body().map(b -> body(b, endpoint.headers())).orElse(null);

		HTTPRequest<T> request = new DefaultHTTPRequest<>(endpoint.path(), endpoint.method(), body, endpoint.headers(),
				endpoint.returnType(), httpClient, codecs, failure);

		return intercepts(Promise.pending(() -> request, executor)).bind(HTTPRequest::execute);
	}

	private <T> Promise<HTTPRequest<T>> intercepts(Promise<HTTPRequest<T>> request) {
		return interceptor.intercepts(request);
	}

	private HTTPRequestBody body(Body content, HTTPHeaders headers) {
		return content.contentType()
				.or(() -> headers.select(CONTENT_TYPE)
					.map(HTTPHeader::value)
					.map(MediaType::valueOf))
				.map(contentType -> codecs.writers().select(contentType, content.javaType())
						.map(writer -> writer.write(content.content(), encoding(contentType)))
						.orElseThrow(() -> new HTTPRequestWriterException(
								format("There isn't any HTTPRequestWriter able to convert {0} to {1}", content.javaType(), contentType))))
				.orElseThrow(() -> new HTTPRequestWriterException(
						"The HTTP request has a body, but doesn't have a Content-Type header."));
	}

	private Charset encoding(MediaType contentType) {
		return contentType.parameter("charset")
				.map(Charset::forName)
				.orElse(encoding);
	}
}
