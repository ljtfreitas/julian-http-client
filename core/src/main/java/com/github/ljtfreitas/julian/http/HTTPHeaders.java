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
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.unmodifiableMap;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.reducing;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toUnmodifiableList;

public class HTTPHeaders implements Iterable<HTTPHeader> {

	private final Map<String, HTTPHeader> headers;

	private HTTPHeaders() {
		this.headers = emptyMap();
	}

	private HTTPHeaders(Map<String, HTTPHeader> headers) {
		this.headers = unmodifiableMap(headers);
	}

	public HTTPHeaders(Collection<HTTPHeader> headers) {
		this(headers.stream().collect(toMap(HTTPHeader::name, identity(), HTTPHeader::join)));
	}

	public Optional<HTTPHeader> select(String name) {
		return headers.entrySet().stream()
				.filter(e -> e.getKey().equalsIgnoreCase(name))
				.map(Map.Entry::getValue)
				.findFirst();
	}

	public Collection<HTTPHeader> all() {
		return headers.values().stream().collect(toUnmodifiableList());
	}

	public HTTPHeaders join(HTTPHeader header) {
		Map<String, HTTPHeader> headers = new LinkedHashMap<>(this.headers);
		headers.put(header.name(), header);
		return new HTTPHeaders(headers);
	}

	@Override
	public Iterator<HTTPHeader> iterator() {
		return headers.values().iterator();
	}

	public static HTTPHeaders empty() {
		return new HTTPHeaders();
	}

	public static HTTPHeaders create(HTTPHeader... headers) {
		return new HTTPHeaders(asList(headers));
	}
}
