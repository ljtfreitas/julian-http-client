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
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class CollectionResponseT implements ResponseT<Collection<Object>, Collection<Object>> {

	private static final CollectionResponseT SINGLE_INSTANCE = new CollectionResponseT();

	@Override
	public <A> ResponseFn<A, Collection<Object>> bind(Endpoint endpoint, ResponseFn<A, Collection<Object>> next) {
		return new ResponseFn<>() {

			@Override
			public Promise<Collection<Object>> run(Promise<? extends Response<A, ? extends Throwable>> response, Arguments arguments) {
				return next.run(response, arguments).then(c -> Optional.ofNullable(c).map(this::collectionByType).orElseGet(this::emptyCollectionByType));
			}

			private Collection<Object> collectionByType(Collection<Object> source) {
				JavaType returnType = endpoint.returnType();

				return returnType.<Collection<Object>>when(List.class, () -> List.copyOf(source))
						.or(() -> returnType.when(Set.class, () -> Set.copyOf(source)))
						.orElse(source);
			}

			private Collection<Object> emptyCollectionByType() {
				JavaType returnType = endpoint.returnType();

				return returnType.<Collection<Object>>when(List.class, Collections::emptyList)
						.or(() -> returnType.when(Set.class, Collections::emptySet))
						.orElseGet(Collections::emptyList);
			}

			@Override
			public JavaType returnType() {
				return next.returnType();
			}
		};
	}

	@Override
	public boolean test(Endpoint endpoint) {
		return endpoint.returnType().is(Collection.class)
			|| endpoint.returnType().is(List.class)
			|| endpoint.returnType().is(Set.class);
	}

	@Override
	public JavaType adapted(Endpoint endpoint) {
		return JavaType.parameterized(Collection.class, endpoint.returnType().parameterized().map(JavaType.Parameterized::firstArg).orElse(Object.class));
	}

	public static CollectionResponseT get() {
		return SINGLE_INSTANCE;
	}
}
