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

class Responses {
	
	private final Collection<ResponseT<?, ?>> requests;

	private final ResponseT<Object, Object> defaultResponseT = new DefaultResponseT<>();

	Responses(Collection<ResponseT<?, ?>> requests) {
		this.requests = requests;
	}

	<M, T> ResponseFn<M, T> select(Endpoint endpoint) {
		return select(endpoint, new Exclusions());
	}
	
	private <M, T> ResponseFn<M, T> select(Endpoint endpoint, Exclusions exclusions) {
		Optional<ResponseT<?, ?>> requestT = find(endpoint, exclusions);

		return requestT.map(r -> this.<M, T> compose(endpoint, r, exclusions)).orElseGet(() -> unsafe(endpoint));
	}

	private Optional<ResponseT<?, ?>> find(Endpoint endpoint, Exclusions exclusions) {
		return requests.stream()
				.filter(exclusions::negate)
				.filter(r -> r.test(endpoint))
				.findFirst();
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private <M, T> ResponseFn<M, T> compose(Endpoint endpoint, ResponseT responseT, Exclusions exclusions) {
		return responseT.comp(endpoint, select(endpoint.returns(responseT.adapted(endpoint)), exclusions.add(responseT)));
	}

	@SuppressWarnings("unchecked")
	private <M, T> ResponseFn<M, T> unsafe(Endpoint endpoint) {
		return (ResponseFn<M, T>) defaultResponseT.comp(endpoint, null);
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
