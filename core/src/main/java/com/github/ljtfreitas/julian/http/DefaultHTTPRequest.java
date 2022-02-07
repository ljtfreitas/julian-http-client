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
import java.util.Optional;

import com.github.ljtfreitas.julian.JavaType;
import com.github.ljtfreitas.julian.Preconditions;
import com.github.ljtfreitas.julian.Promise;
import com.github.ljtfreitas.julian.http.client.HTTPClient;
import com.github.ljtfreitas.julian.http.codec.HTTPMessageCodecs;

class DefaultHTTPRequest<T> implements HTTPRequest<T> {

	private final JavaType returnType;
	private final URI path;
	private final HTTPMethod method;
	private final HTTPRequestBody body;
	private final HTTPHeaders headers;
	private final HTTPRequestIO<T> io;

	public DefaultHTTPRequest(URI path, HTTPMethod method, HTTPRequestBody body, HTTPHeaders headers, JavaType returnType,
							  HTTPClient httpClient, HTTPMessageCodecs codecs, HTTPResponseFailure failure) {
		this.returnType = returnType;
		this.path = path;
		this.method = method;
		this.body = body;
		this.headers = headers;
		this.io = new DefaultHTTPRequestIO<>(this, httpClient, codecs, failure);
	}

	private DefaultHTTPRequest(URI path, HTTPMethod method, HTTPRequestBody body, HTTPHeaders headers, JavaType returnType, HTTPRequestIO<T> io) {
		this.returnType = returnType;
		this.path = path;
		this.method = method;
		this.body = body;
		this.headers = headers;
		this.io = io;
	}

	@Override
	public URI path() {
		return path;
	}

	@Override
	public HTTPMethod method() {
		return method;
	}
	
	@Override
	public HTTPHeaders headers() {
		return headers;
	}

	@Override
	public Optional<HTTPRequestBody> body() {
		return Optional.ofNullable(body);
	}
	
	@Override
	public JavaType returnType() {
		return returnType;
	}

	@Override
	public Promise<HTTPResponse<T>> execute() {
		return io.execute();
	}

	@Override
	public HTTPRequest<T> path(URI path) {
		return new DefaultHTTPRequest<>(path, method, body, headers, returnType, io);
	}

	@Override
	public HTTPRequest<T> method(HTTPMethod method) {
		return new DefaultHTTPRequest<>(path, method, body, headers, returnType, io);
	}

	@Override
	public HTTPRequest<T> headers(HTTPHeaders headers) {
		return new DefaultHTTPRequest<>(path, method, body, headers, returnType, io);
	}

	@Override
	public HTTPRequest<T> body(HTTPRequestBody body) {
		return new DefaultHTTPRequest<>(path, method, body, headers, returnType, io);
	}
}
