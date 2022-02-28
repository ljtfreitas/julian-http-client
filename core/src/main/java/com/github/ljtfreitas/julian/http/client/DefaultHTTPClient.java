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

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;

import com.github.ljtfreitas.julian.http.HTTPRequest;
import com.github.ljtfreitas.julian.http.HTTPRequestDefinition;
import com.github.ljtfreitas.julian.http.codec.HTTPMessageCodecs;

import static java.net.http.HttpRequest.BodyPublishers.fromPublisher;

public class DefaultHTTPClient implements HTTPClient {

	private final HttpClient client;
	private final HTTPClient.Specification specification;

	public DefaultHTTPClient() {
		this(HttpClient.newHttpClient());
	}

	public DefaultHTTPClient(HttpClient client) {
		this.client = client;
		this.specification = new HTTPClient.Specification();
	}

	public DefaultHTTPClient(HTTPClient.Specification specification) {
		HttpClient.Builder builder = HttpClient.newBuilder();

		specification.connectionTimeout().ifPresent(builder::connectTimeout);
		specification.redirects().apply(() -> builder.followRedirects(HttpClient.Redirect.NORMAL),
										() -> builder.followRedirects(HttpClient.Redirect.NEVER));
		specification.proxySelector().ifPresent(builder::proxy);
		specification.ssl().context().ifPresent(builder::sslContext);
		specification.ssl().parameters().ifPresent(builder::sslParameters);

		this.client = builder.build();
		this.specification = specification;
	}

	@Override
	public HTTPClientRequest request(HTTPRequestDefinition request) {
		HttpRequest.Builder builder = HttpRequest.newBuilder(request.path())
				.method(request.method().name(), request.body().map(b -> fromPublisher(b.serialize())).orElseGet(BodyPublishers::noBody));

		request.headers().forEach(header -> header.values().forEach(value -> builder.header(header.name(), value)));

		specification.requestTimeout().ifPresent(builder::timeout);

		return new DefaultHTTPClientRequest(client, builder.build());
	}
}
