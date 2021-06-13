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

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableCollection;
import static java.util.stream.Collectors.toUnmodifiableList;

public class Headers implements Iterable<Header> {

	private final Collection<Header> headers;

	public Headers(Collection<Header> headers) {
		this.headers = unmodifiableCollection(headers);
	}

	private Headers(Stream<Header> headers) {
		this.headers = headers.collect(toUnmodifiableList());
	}

	public Headers merge(Headers that) {
		return new Headers(Stream.concat(this.headers.stream(), that.headers.parallelStream()));
	}

	public Headers merge(Stream<Header> headers) {
		return new Headers(Stream.concat(this.headers.stream(), headers));
	}

	public Headers add(Header... headers) {
		return new Headers(Stream.concat(this.headers.stream(), Arrays.stream(headers)));
	}

	public <R> Stream<R> map(Function<Header, R> fn) {
		return headers.stream().map(fn);
	}

	public Collection<Header> all() {
		return headers;
	}

	@Override
	public Iterator<Header> iterator() {
		return headers.iterator();
	}

	@Override
	public String toString() {
		return headers.toString();
	}

	public static Headers create(Header... headers) {
		return new Headers(asList(headers));
	}

	public static Headers empty() {
		return new Headers(emptyList());
	}
}
