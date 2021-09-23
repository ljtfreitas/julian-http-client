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

public class HTTPResponseException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	private final HTTPStatus status;
	private final HTTPHeaders headers;
	private final String body;

	HTTPResponseException(HTTPStatus status, HTTPHeaders headers, String body) {
		super(HTTPResponseException.message(status, headers, body));
		this.status = status;
		this.headers = headers;
		this.body = body;
	}

	public HTTPStatus status() {
		return status;
	}

	public HTTPHeaders headers() {
		return headers;
	}

	public String body() {
		return body;
	}

	private static String message(HTTPStatus status, HTTPHeaders headers, String body) {
		return new StringBuilder()
				.append(status)
					.append("\n")
				.append("headers: ")
					.append("[")
						.append(headers)
					.append("]")
				.append("\n")
				.append(body)
				.toString();
	}
}
