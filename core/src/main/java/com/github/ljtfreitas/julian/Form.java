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

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import static com.github.ljtfreitas.julian.Preconditions.nonNull;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.unmodifiableMap;
import static java.util.stream.Collectors.flatMapping;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toUnmodifiableList;

public class Form {

	private final Map<String, Collection<String>> parameters;

	public Form() {
		this.parameters = emptyMap();
	}

	public Form(Map<String, ? extends Collection<String>> parameters) {
		this.parameters = unmodifiableMap(parameters);
	}

	@Override
	public String toString() {
		return serialize();
	}

	public Optional<String> select(String name) {
		return parameters.getOrDefault(name, emptyList()).stream().findFirst();
	}

	public Collection<String> all(String name) {
		return parameters.getOrDefault(name, emptyList());
	}

	public Map<String, Collection<String>> all() {
		return parameters;
	}

	public String serialize() {
		return parameters.entrySet().stream()
				.flatMap(e -> e.getValue().stream().map(value -> encode(e.getKey()) + "=" + encode(value)))
				.collect(joining("&"));
	}

	private String encode(String value) {
		return Except.run(() -> URLEncoder.encode(value, UTF_8))
				.recover(UnsupportedEncodingException.class, () -> value)
				.unsafe();
	}

	public Form join(String name, String... values) {
		Map<String, Collection<String>> parameters = new LinkedHashMap<>(this.parameters);

		parameters.merge(name, asList(values), (a, b) -> {
			Collection<String> e = new ArrayList<>(a);
			e.addAll(b);
			return e;
		});

		return new Form(parameters);
	}

	public Form join(String source) {
		Map<String, Collection<String>> parameters = new LinkedHashMap<>(this.parameters);

		asMap(source).forEach((name, values) -> parameters.merge(name, values, (a, b) -> {
			Collection<String> e = new ArrayList<>(a);
			e.addAll(b);
			return e;
		}));

		return new Form(parameters);
	}

	public static Form empty() {
		return new Form(emptyMap());
	}

	public static Form create(String name, String... values) {
		return new Form(Map.of(name, asList(values)));
	}

	public static Form create(Map<String, ? extends Collection<Object>> parameters) {
		return new Form(nonNull(parameters).entrySet().stream()
				.map(e -> Map.entry(e.getKey(), e.getValue().stream().map(Object::toString).collect(toUnmodifiableList())))
				.collect(groupingBy(Entry::getKey, flatMapping(e -> e.getValue().stream(), toUnmodifiableList()))));
	}

	public static Form parse(String source) {
		return new Form(asMap(source));
	}

	private static Map<String, List<String>> asMap(String source) {
		return Arrays.stream(nonNull(source).split("&"))
				.map(parameter -> parameter.split("="))
				.filter(parameter -> parameter.length == 2)
				.collect(groupingBy(parameter -> parameter[0], mapping(parameter -> parameter[1], toUnmodifiableList())));
	}
}
