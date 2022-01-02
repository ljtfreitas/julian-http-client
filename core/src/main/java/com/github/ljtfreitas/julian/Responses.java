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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

class Responses {

	private final ResponseT<Object, Object> defaultResponseT = ObjectResponseT.get();

	private final ConcurrentHashMap<Endpoint, ResponseFn<?, ?>> cache;
	private final Collection<ResponseT<?, ?>> responses;

	Responses(Collection<ResponseT<?, ?>> responses) {
		this.cache = new ConcurrentHashMap<>();
		this.responses = responses;
	}

	@SuppressWarnings("unchecked")
	<M, T> ResponseFn<T, M> select(Endpoint endpoint) {
		return (ResponseFn<T, M>) cache.computeIfAbsent(endpoint, e -> select(e, new Exclusions()));
	}
	
	private <M, T> ResponseFn<T, M> select(Endpoint endpoint, Exclusions exclusions) {
		Optional<ResponseT<?, ?>> responseT = find(endpoint, exclusions);

		return responseT.map(r -> this.<M, T> compose(endpoint, r, exclusions)).orElseGet(() -> unsafe(endpoint));
	}

	private Optional<ResponseT<?, ?>> find(Endpoint endpoint, Exclusions exclusions) {
		return responses.stream()
				.filter(exclusions::negate)
				.filter(r -> r.test(endpoint))
				.findFirst();
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private <M, T> ResponseFn<T, M> compose(Endpoint endpoint, ResponseT responseT, Exclusions exclusions) {
		return responseT.bind(endpoint, select(endpoint.returns(responseT.adapted(endpoint)), exclusions.add(responseT)));
	}

	@SuppressWarnings("unchecked")
	private <M, T> ResponseFn<T, M> unsafe(Endpoint endpoint) {
		return (ResponseFn<T, M>) defaultResponseT.bind(endpoint, null);
	}
	
	private class Exclusions {
	
		private final Collection<ResponseT<?, ?>> exclusions = new ArrayList<>();

		private Exclusions add(ResponseT<?, ?> requestT) {
			exclusions.add(requestT);
			return this;
		}
		
		private boolean negate(ResponseT<?, ?> requestT) {
			return !exclusions.contains(requestT);
		}
	}
}
