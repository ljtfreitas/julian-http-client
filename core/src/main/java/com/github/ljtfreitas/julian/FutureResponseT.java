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

import java.util.concurrent.Future;

class FutureResponseT implements ResponseT<Object, Future<Object>> {

	private static final FutureResponseT SINGLE_INSTANCE = new FutureResponseT();

	@Override
	public <A> ResponseFn<A, Future<Object>> bind(Endpoint endpoint, ResponseFn<A, Object> next) {
		return new ResponseFn<>() {

			@Override
			public Future<Object> join(Promise<? extends Response<A, ? extends Throwable>> response, Arguments arguments) {
				return next.run(response, arguments).future();
			}

			@Override
			public JavaType returnType() {
				return next.returnType();
			}
		};
	}

	@Override
	public boolean test(Endpoint endpoint) {
		return endpoint.returnType().is(Future.class);
	}

	@Override
	public JavaType adapted(Endpoint endpoint) {
		return endpoint.returnType().parameterized().map(JavaType.Parameterized::firstArg).map(JavaType::valueOf)
				.orElseGet(JavaType::object);
	}

	public static FutureResponseT get() {
		return SINGLE_INSTANCE;
	}
}
