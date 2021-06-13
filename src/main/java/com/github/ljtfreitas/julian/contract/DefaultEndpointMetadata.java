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

import static java.util.stream.Collectors.flatMapping;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toUnmodifiableList;

import java.lang.reflect.Method;
import java.net.URL;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Stream;

import com.github.ljtfreitas.julian.Cookie;
import com.github.ljtfreitas.julian.Cookies;
import com.github.ljtfreitas.julian.Endpoint;
import com.github.ljtfreitas.julian.EndpointDefinition;
import com.github.ljtfreitas.julian.EndpointDefinition.Parameters;
import com.github.ljtfreitas.julian.Header;
import com.github.ljtfreitas.julian.Headers;
import com.github.ljtfreitas.julian.MethodEndpoint;
import com.github.ljtfreitas.julian.QueryString;
import com.github.ljtfreitas.julian.contract.EndpointMetadata;
import com.github.ljtfreitas.julian.contract.JavaClass;

class DefaultEndpointMetadata implements EndpointMetadata {

	private final JavaClass<?> javaClass;
	private final JavaMethod javaMethod;

	DefaultEndpointMetadata(Class<?> javaClass, Method javaMethod) {
		this.javaClass = JavaClass.valueOf(javaClass);
		this.javaMethod = JavaMethod.create(javaClass, javaMethod);
	}

	@Override
	public Endpoint endpoint(Optional<URL> root) {
		return new MethodEndpoint(definition(root), javaMethod.source());
	}

	private EndpointDefinition definition(Optional<URL> root) {
		return new EndpointDefinition(path(root, javaMethod.parameters()), javaMethod.httpMethod(), headers(), cookies(), javaMethod.parameters(), javaMethod.returnType());
	}

	private Headers headers() {
		return new Headers(Stream.concat(javaClass.headers(), javaMethod.headers())
			.map(e -> new Header(e.getKey(), e.getValue()))
			.collect(toUnmodifiableList()));
	}

	private Cookies cookies() {
		return new Cookies(Stream.concat(javaClass.cookies(), javaMethod.cookies())
				.map(e -> new Cookie(e.getKey(), e.getValue()))
				.collect(toUnmodifiableList()));
	}

	private EndpointDefinition.Path path(Optional<URL> root, Parameters parameters) {
		return new EndpointDefinition.Path(path(root), queryString(), parameters);
	}

	private QueryString queryString() {
		return new QueryString(Stream.concat(javaClass.query(), javaMethod.query())
				.collect(groupingBy(Entry::getKey, flatMapping(e -> e.getValue().stream(), toUnmodifiableList()))));
	}

	private String path(Optional<URL> root) {
		return Stream.concat(root.map(URL::toString).stream(), Stream.concat(javaClass.path(), javaMethod.path().stream()))
				.map(path -> path.startsWith("/") ? path.substring(1) : path)
				.collect(joining("/"));
	}

}
