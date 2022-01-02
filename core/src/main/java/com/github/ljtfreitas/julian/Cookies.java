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
import java.util.Iterator;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableCollection;
import static java.util.stream.Collectors.toUnmodifiableList;

public class Cookies implements Iterable<Cookie> {

	private final Collection<Cookie> cookies;

	public Cookies(Collection<Cookie> cookies) {
		this.cookies = unmodifiableCollection(cookies);
	}

	private Cookies(Stream<Cookie> cookies) {
		this.cookies = cookies.collect(toUnmodifiableList());
	}

	public Cookies merge(Cookies that) {
		return new Cookies(Stream.concat(this.cookies.stream(), that.cookies.stream()));
	}

	public Optional<Header> header() {
		return cookies.isEmpty() ? Optional.empty() : Optional.of(new Header("Cookie", cookies.stream().map(Cookie::pair).collect(Collectors.joining("; "))));
	}

	@Override
	public Iterator<Cookie> iterator() {
		return cookies.iterator();
	}

	public Collection<Cookie> all() {
		return cookies;
	}

	@Override
	public String toString() {
		return cookies.toString();
	}

	public static Cookies empty() {
		return new Cookies(emptyList());
	}

	public static Cookies create(Cookie... cookies) {
		return new Cookies(asList(cookies));
	}
}
