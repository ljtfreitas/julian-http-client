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

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

import com.github.ljtfreitas.julian.Arguments;
import com.github.ljtfreitas.julian.Cookies;
import com.github.ljtfreitas.julian.Endpoint;
import com.github.ljtfreitas.julian.Header;
import com.github.ljtfreitas.julian.Headers;
import com.github.ljtfreitas.julian.JavaType;
import com.github.ljtfreitas.julian.Promise;
import com.github.ljtfreitas.julian.http.client.HTTPClient;
import com.github.ljtfreitas.julian.http.codec.ContentType;
import com.github.ljtfreitas.julian.http.codec.HTTPMessageCodecs;
import com.github.ljtfreitas.julian.http.codec.HTTPRequestWriterException;

import static com.github.ljtfreitas.julian.Message.format;

public class DefaultHTTP implements HTTP {

	private final HTTPClient httpClient;
	private final HTTPMessageCodecs codecs;
	private final HTTPResponseFailure failure;

	public DefaultHTTP(HTTPClient httpClient, HTTPMessageCodecs codecs, HTTPResponseFailure failure) {
		this.httpClient = httpClient;
		this.codecs = codecs;
		this.failure = failure;
	}

	@Override
	public <T> Promise<HTTPRequest<T>> request(Endpoint endpoint, Arguments arguments, JavaType returnType) {
		URI uri = endpoint.path().expand(arguments)
				.prop(cause -> new IllegalArgumentException(endpoint.path().toString(), cause));

		HTTPHeaders headers = HTTPHeaders.valueOf(headers(endpoint, arguments).merge(cookies(endpoint, arguments)));

		HTTPRequestBody body = body(endpoint, arguments, headers);

		HTTPMethod method = HTTPMethod.select(endpoint.method())
				.orElseThrow(() -> new IllegalArgumentException(format("Unsupported HTTP method: {0}", endpoint.method())));

		return Promise.done(new DefaultHTTPRequest<>(returnType, uri, method, body, headers, httpClient, codecs, failure));
	}

	private HTTPRequestBody body(Endpoint endpoint, Arguments arguments, HTTPHeaders headers) {
		return endpoint.parameters().body()
				.flatMap(p -> arguments.of(p.position()))
				.map(b -> headers.select(HTTPHeader.CONTENT_TYPE).map(HTTPHeader::value)
							.map(ContentType::valueOf)
							.map(contentType -> codecs.writers().select(contentType, b.getClass())
									.orElseThrow(() -> new HTTPRequestWriterException(format("There isn't any HTTPRequestWriter able to convert {0} to {1}", b.getClass(), contentType))))
							.map(writer -> new DefaultHTTPRequestBody(b, StandardCharsets.UTF_8, writer))
							.orElseThrow(() -> new HTTPRequestWriterException("The HTTP request has a body, but doesn't have a Content-Type header.")))
				.orElse(null);
	}

	private Headers headers(Endpoint endpoint, Arguments arguments) {
		return endpoint.headers()
				.merge(endpoint.parameters().headers()
						.flatMap(header -> arguments.of(header.position())
								.flatMap(header::resolve)
								.stream())
						.reduce(Headers::merge).orElseGet(Headers::empty));
	}

	private Stream<Header> cookies(Endpoint endpoint, Arguments arguments) {
		return endpoint.cookies()
				.merge(endpoint.parameters().cookies()
						.flatMap(cookie -> arguments.of(cookie.position())
								.flatMap(cookie::resolve)
								.stream())
						.reduce(Cookies::merge).orElseGet(Cookies::empty))
				.header()
				.stream();
	}
}
