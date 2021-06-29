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

import java.util.Collection;
import java.util.Iterator;
import java.util.Optional;
import java.util.stream.Stream;

import com.github.ljtfreitas.julian.Headers;

import static java.util.stream.Collectors.toUnmodifiableList;

public class HTTPHeaders implements Iterable<HTTPHeader> {

	private final Collection<HTTPHeader> headers;

	private HTTPHeaders(Stream<HTTPHeader> headers) {
		this.headers = headers.collect(toUnmodifiableList());
	}

	Optional<HTTPHeader> select(String name) {
		return headers.stream().filter(h -> h.is(name)).findFirst();
	}

	Collection<HTTPHeader> all() {
		return headers;
	}

	public HTTPHeaders add(HTTPHeader header) {
		return new HTTPHeaders(Stream.concat(headers.stream(), Stream.of(header)));
	}

	@Override
	public Iterator<HTTPHeader> iterator() {
		return headers.iterator();
	}

	public static HTTPHeaders empty() {
		return new HTTPHeaders(Stream.empty());
	}

	static HTTPHeaders valueOf(Headers headers) {
		return new HTTPHeaders(headers.map(header -> new HTTPHeader(header.name(), header.values())));
	}
}
