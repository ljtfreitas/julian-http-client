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

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Optional;

class IteratorResponseT implements ResponseT<Collection<Object>, Iterator<Object>> {

	private static final IteratorResponseT SINGLE_INSTANCE = new IteratorResponseT();

	@Override
	public <A> ResponseFn<A, Iterator<Object>> bind(Endpoint endpoint, ResponseFn<A, Collection<Object>> next) {
		return new ResponseFn<>() {

			@Override
			public Promise<Iterator<Object>> run(Promise<? extends Response<A>> response, Arguments arguments) {
				return next.run(response, arguments).then(c -> Optional.ofNullable(c).map(Collection::iterator).orElseGet(Collections::emptyIterator));
			}

			@Override
			public JavaType returnType() {
				return next.returnType();
			}
		};
	}
	
	@Override
	public boolean test(Endpoint endpoint) {
		return endpoint.returnType().is(Iterator.class);
	}

	@Override
	public JavaType adapted(Endpoint endpoint) {
		return JavaType.parameterized(Collection.class, endpoint.returnType().parameterized().map(JavaType.Parameterized::firstArg).orElse(Object.class));
	}

	public static IteratorResponseT get() {
		return SINGLE_INSTANCE;
	}
}
