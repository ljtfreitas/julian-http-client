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

package com.github.ljtfreitas.julian;

import com.github.ljtfreitas.julian.http.HTTP;
import com.github.ljtfreitas.julian.http.HTTPResponse;

class Client {

	private final Responses responses;
	private final HTTP http;

	Client(Responses responses, HTTP http) {
		this.responses = responses;
		this.http = http;
	}

	<T> T run(Endpoint endpoint, Arguments arguments) {
		ResponseFn<Object, T> responseFn = responses.select(endpoint);

		RequestDefinition request = endpoint.request(arguments, responseFn.returnType());

		Promise<HTTPResponse<Object>> response = http.run(request);

		return responseFn.join(response, arguments);
	}
}
