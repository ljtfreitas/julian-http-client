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

import static java.net.http.HttpRequest.BodyPublishers.ofInputStream;

public class DefaultHTTPClient implements HTTPClient {

	private final HttpClient client = HttpClient.newHttpClient();

	@Override
	public HTTPClientRequest request(HTTPRequest<?> request) {
		HttpRequest.Builder builder = HttpRequest.newBuilder(request.path())
				.method(request.method().name(), request.body().map(b -> ofInputStream(b::serialize)).orElseGet(BodyPublishers::noBody));

		request.headers().forEach(header -> header.values().forEach(value -> builder.header(header.name(), value)));

		return new DefaultHTTPClientRequest(client, builder.build());
	}
}
