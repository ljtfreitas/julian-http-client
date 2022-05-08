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
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toUnmodifiableList;

public class QueryParameters {

	private final MultiMap<String, String> parameters;

	public QueryParameters() {
		this(MultiMap.empty());
	}

	public QueryParameters(Map<String, ? extends Collection<String>> parameters) {
		this(new MultiMap<>(parameters));
	}

	private QueryParameters(MultiMap<String, String> parameters) {
		this.parameters = parameters;
	}

	public Map<String, Collection<String>> all() {
		return parameters.all();
	}

	public String serialize() {
		return parameters.serialize((name, value) -> encode(name) + "=" + encode(value), "&");
	}

	private String encode(String value) {
		return Attempt.run(() -> URLEncoder.encode(value, UTF_8))
				.recover(UnsupportedEncodingException.class, () -> value)
				.unsafe();
	}

	public QueryParameters join(String name, String... values) {
		return new QueryParameters(parameters.join(name, values));
	}

	public QueryParameters join(QueryParameters that) {
		return new QueryParameters(parameters.join(that.parameters));
	}

	public static QueryParameters empty() {
		return new QueryParameters(MultiMap.empty());
	}

	public static QueryParameters create(String name, String... values) {
		return new QueryParameters(Map.of(name, asList(values)));
	}

	public static QueryParameters create(Map<String, String> parameters) {
		return new QueryParameters(MultiMap.valueOf(parameters));
	}

	public static QueryParameters parse(String source) {
		return new QueryParameters(Arrays.stream((source == null ? "" : source).split("&"))
				.map(parameter -> parameter.split("="))
				.filter(parameter -> parameter.length == 2)
				.collect(groupingBy(parameter -> parameter[0], mapping(parameter -> parameter[1], toUnmodifiableList()))));
	}
}
