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

class RunnableResponseT implements ResponseT<Void, Runnable> {

	private static final RunnableResponseT SINGLE_INSTANCE = new RunnableResponseT();

	@Override
	public <A> ResponseFn<A, Runnable> bind(Endpoint endpoint, ResponseFn<A, Void> fn) {
		return new ResponseFn<A, Runnable>() {

			@Override
			public Runnable join(Promise<? extends Response<A>> response, Arguments arguments) {
				return () -> fn.join(response, arguments);
			}

			@Override
			public JavaType returnType() {
				return fn.returnType();
			}
		};
	}

	@Override
	public boolean test(Endpoint endpoint) {
		return endpoint.returnType().is(Runnable.class);
	}

	@Override
	public JavaType adapted(Endpoint endpoint) {
		return JavaType.none();
	}

	public static RunnableResponseT get() {
		return SINGLE_INSTANCE;
	}
}
