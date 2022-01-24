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

import com.github.ljtfreitas.julian.Arguments;
import com.github.ljtfreitas.julian.Cookies;
import com.github.ljtfreitas.julian.Endpoint;
import com.github.ljtfreitas.julian.Endpoint.BodyParameter;
import com.github.ljtfreitas.julian.Header;
import com.github.ljtfreitas.julian.Headers;
import com.github.ljtfreitas.julian.JavaType;
import com.github.ljtfreitas.julian.Promise;
import com.github.ljtfreitas.julian.RequestDefinition;
import com.github.ljtfreitas.julian.http.client.HTTPClient;
import com.github.ljtfreitas.julian.http.codec.HTTPMessageCodecs;
import com.github.ljtfreitas.julian.http.codec.HTTPRequestWriterException;

import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

import static com.github.ljtfreitas.julian.Message.format;
import static com.github.ljtfreitas.julian.http.HTTPHeader.CONTENT_TYPE;

public class DefaultHTTP implements HTTP {

	private final HTTPClient httpClient;
	private final HTTPRequestInterceptor interceptor;
	private final HTTPMessageCodecs codecs;
	private final HTTPResponseFailure failure;
	private final Charset encoding;

	public DefaultHTTP(HTTPClient httpClient, HTTPRequestInterceptor interceptor, HTTPMessageCodecs codecs, HTTPResponseFailure failure) {
		this(httpClient, interceptor, codecs, failure, StandardCharsets.UTF_8);
	}

	public DefaultHTTP(HTTPClient httpClient, HTTPRequestInterceptor interceptor, HTTPMessageCodecs codecs, HTTPResponseFailure failure, Charset encoding) {
		this.httpClient = httpClient;
		this.interceptor = interceptor;
		this.codecs = codecs;
		this.failure = failure;
		this.encoding = encoding;
	}

	@Override
	public <T> Promise<HTTPResponse<T>, HTTPException> run(RequestDefinition definition) {
		URI uri = definition.path();

		HTTPHeaders headers = definition.headers().all().stream()
				.map(h -> new HTTPHeader(h.name(), h.values()))
				.reduce(HTTPHeaders.empty(), HTTPHeaders::join, (a, b) -> b);

		HTTPRequestBody body = definition.body().map(b -> body(b.content(), b.javaType(), headers)).orElse(null);

		HTTPMethod method = HTTPMethod.select(definition.method())
				.orElseThrow(() -> new IllegalArgumentException(format("Unsupported HTTP method: {0}", definition.method())));

		HTTPRequest<T> request = new DefaultHTTPRequest<>(uri, method, body, headers, definition.returnType(), httpClient, codecs, failure);

		return intercepts(Promise.pending(() -> request)).bind(HTTPRequest::execute);
	}

	private <T> Promise<HTTPRequest<T>, HTTPException> intercepts(Promise<HTTPRequest<T>, HTTPException> request) {
		return interceptor.intercepts(request);
	}

	private HTTPRequestBody body(Object content, JavaType javaType, HTTPHeaders headers) {
		return headers.select(CONTENT_TYPE)
				.map(HTTPHeader::value)
				.map(MediaType::valueOf)
				.map(contentType -> codecs.writers().select(contentType, javaType)
						.map(writer -> writer.write(content, encoding(contentType)))
						.orElseThrow(() -> new HTTPRequestWriterException(format("There isn't any HTTPRequestWriter able to convert {0} to {1}", javaType, contentType))))
				.orElseThrow(() -> new HTTPRequestWriterException("The HTTP request has a body, but doesn't have a Content-Type header."));
	}

	private Charset encoding(MediaType contentType) {
		return contentType.parameter("charset")
				.map(Charset::forName)
				.orElse(encoding);
	}
}
