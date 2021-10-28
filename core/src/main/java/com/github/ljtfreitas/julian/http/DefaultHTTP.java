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
import com.github.ljtfreitas.julian.http.client.HTTPClient;
import com.github.ljtfreitas.julian.http.codec.HTTPMessageCodecs;
import com.github.ljtfreitas.julian.http.codec.HTTPRequestWriterException;

import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.stream.Stream;

import static com.github.ljtfreitas.julian.Message.format;
import static com.github.ljtfreitas.julian.http.HTTPHeader.CONTENT_TYPE;

public class DefaultHTTP implements HTTP {

	private final HTTPClient httpClient;
	private final HTTPMessageCodecs codecs;
	private final HTTPResponseFailure failure;
	private final Charset encoding;

	public DefaultHTTP(HTTPClient httpClient, HTTPMessageCodecs codecs, HTTPResponseFailure failure) {
		this(httpClient, codecs, failure, StandardCharsets.UTF_8);
	}

	public DefaultHTTP(HTTPClient httpClient, HTTPMessageCodecs codecs, HTTPResponseFailure failure, Charset encoding) {
		this.httpClient = httpClient;
		this.codecs = codecs;
		this.failure = failure;
		this.encoding = encoding;
	}

	@Override
	public <T> Promise<HTTPRequest<T>> request(Endpoint endpoint, Arguments arguments, JavaType returnType) {
		URI uri = endpoint.path().expand(arguments)
				.prop(cause -> new IllegalArgumentException(endpoint.path().show(), cause));

		HTTPHeaders headers = headers(endpoint, arguments).merge(cookies(endpoint, arguments)).all().stream()
				.map(h -> new HTTPHeader(h.name(), h.values()))
				.reduce(HTTPHeaders.empty(), HTTPHeaders::join, (a, b) -> b);

		HTTPRequestBody body = body(endpoint, arguments, headers);

		HTTPMethod method = HTTPMethod.select(endpoint.method())
				.orElseThrow(() -> new IllegalArgumentException(format("Unsupported HTTP method: {0}", endpoint.method())));

		return Promise.pending(() -> new DefaultHTTPRequest<>(returnType, uri, method, body, headers, httpClient, codecs, failure));
	}

	private HTTPRequestBody body(Endpoint endpoint, Arguments arguments, HTTPHeaders headers) {
		return endpoint.parameters().body()
				.flatMap(p -> arguments.of(p.position())
					.map(b -> p.contentType()
						.map(MediaType::valueOf)
						.or(() -> headers.select(CONTENT_TYPE)
								.map(HTTPHeader::value)
								.map(MediaType::valueOf))
						.map(contentType -> codecs.writers().select(contentType, p.javaType())
								.map(writer -> writer.write(b, encoding(contentType)))
								.orElseThrow(() -> new HTTPRequestWriterException(format("There isn't any HTTPRequestWriter able to convert {0} to {1}", b.getClass(), contentType))))
						.orElseThrow(() -> new HTTPRequestWriterException("The HTTP request has a body, but doesn't have a Content-Type header."))))
				.orElse(null);
	}

	private Charset encoding(MediaType contentType) {
		return contentType.parameter("charset")
				.map(Charset::forName)
				.orElse(encoding);
	}

	private Headers headers(Endpoint endpoint, Arguments arguments) {
		Stream<Headers> bodyContentType = endpoint.parameters().body()
				.flatMap(BodyParameter::contentType)
				.map(c -> new Header(CONTENT_TYPE, c))
				.map(Headers::create)
				.stream();

		Stream<Headers> parameters = endpoint.parameters().headers()
				.flatMap(header -> arguments.of(header.position())
						.flatMap(header::resolve)
						.stream());

		return endpoint.headers()
				.merge(Stream.concat(bodyContentType, parameters)
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
