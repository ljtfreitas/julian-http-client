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

public interface HTTPRequest<T> extends HTTPRequestDefinition, HTTPRequestIO<T> {

	HTTPRequest<T> path(URI path);

	HTTPRequest<T> method(HTTPMethod method);

	HTTPRequest<T> headers(HTTPHeaders headers);

	HTTPRequest<T> body(HTTPRequestBody body);

	static HTTPRequestBuilder.HasHeaders<Void> GET(String path) {
		return new HTTPRequestBuilder.HasHeaders<>(path, HTTPMethod.GET);
	}

	static HTTPRequestBuilder.HasBody<Void> POST(String path) {
		return new HTTPRequestBuilder.HasBody<>(path, HTTPMethod.POST);
	}

	static HTTPRequestBuilder.HasBody<Void> PUT(String path) {
		return new HTTPRequestBuilder.HasBody<>(path, HTTPMethod.PUT);
	}

	static HTTPRequestBuilder.HasBody<Void> PATCH(String path) {
		return new HTTPRequestBuilder.HasBody<>(path, HTTPMethod.PATCH);
	}

	static HTTPRequestBuilder.HasBody<Void> DELETE(String path) {
		return new HTTPRequestBuilder.HasBody<>(path, HTTPMethod.DELETE);
	}

	static HTTPRequestBuilder.JustHeaders<Void> HEAD(String path) {
		return new HTTPRequestBuilder.JustHeaders<>(path, HTTPMethod.HEAD);
	}

	static HTTPRequestBuilder.JustHeaders<Void> OPTIONS(String path) {
		return new HTTPRequestBuilder.JustHeaders<>(path, HTTPMethod.OPTIONS);
	}

	static HTTPRequestBuilder.JustHeaders<Void> TRACE(String path) {
		return new HTTPRequestBuilder.JustHeaders<>(path, HTTPMethod.TRACE);
	}
}
