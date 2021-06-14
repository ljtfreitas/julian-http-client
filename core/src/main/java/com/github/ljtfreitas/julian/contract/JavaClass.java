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

package com.github.ljtfreitas.julian.contract;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static com.github.ljtfreitas.julian.Message.format;
import static com.github.ljtfreitas.julian.Preconditions.nonNull;
import static java.util.stream.Collectors.toUnmodifiableList;

class JavaClass<T> {

	private final Class<T> source;
	private final Optional<JavaClass<?>> parent;
	private final Optional<Path> path;
	private final Collection<Header> headers;
	private final Collection<Cookie> cookies;
	private final Collection<QueryParameter> queryParameters;

	private JavaClass(Class<T> source, Optional<JavaClass<?>> parent, Optional<Path> path, Collection<Header> headers,
					  Collection<Cookie> cookies, Collection<QueryParameter> queryParameters) {
		this.source = source;
		this.parent = parent;
		this.path = path;
		this.headers = headers;
		this.cookies = cookies;
		this.queryParameters = queryParameters;
	}

	Stream<String> path() {
		return Stream.concat(
					parent.map(JavaClass::path).orElseGet(Stream::empty),
					path.map(Path::value).stream())
				.filter(Predicate.not(String::isEmpty));
	}

	Stream<Entry<String, Collection<String>>> headers() {
		return Stream.concat(
				parent.map(JavaClass::headers).orElseGet(Stream::empty),
				headers.stream().map(h -> Map.entry(h.name(), Arrays.asList(h.value()))));
	}

	Stream<Entry<String, String>> cookies() {
		return Stream.concat(
				parent.map(JavaClass::cookies).orElseGet(Stream::empty),
				cookies.stream().map(c -> Map.entry(c.name(), c.value())));
	}

	Stream<Entry<String, Collection<String>>> query() {
		return Stream.concat(
				parent.map(JavaClass::query).orElseGet(Stream::empty),
				queryParameters.stream().map(q -> Map.entry(q.name(), Arrays.asList(q.value()))));
	}

	static <T> JavaClass<T> valueOf(Class<T> type) {
        Scannotation scannotation = new Scannotation(type);

		return new JavaClass<>(nonNull(type), parent(type), scannotation.find(Path.class),
				scannotation.scan(Header.class).collect(toUnmodifiableList()),
				scannotation.scan(Cookie.class).collect(toUnmodifiableList()),
				scannotation.scan(QueryParameter.class).collect(toUnmodifiableList()));
	}

	private static Optional<JavaClass<?>> parent(Class<?> source) {
		switch (source.getInterfaces().length) {
			case 0:
				return Optional.empty();

			case 1:
				return Optional.of(JavaClass.valueOf(source.getInterfaces()[0]));

			default:
				throw new IllegalArgumentException(format("{0} extends {1} interfaces; only single-level inheritance is supported.", source, source.getInterfaces().length)) ;
		}
	}
}
