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
import java.util.Collection;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Stream;

import com.github.ljtfreitas.julian.Cookie;
import com.github.ljtfreitas.julian.Cookies;
import com.github.ljtfreitas.julian.Endpoint;
import com.github.ljtfreitas.julian.Endpoint.Parameters;
import com.github.ljtfreitas.julian.Header;
import com.github.ljtfreitas.julian.Headers;
import com.github.ljtfreitas.julian.MethodEndpoint;
import com.github.ljtfreitas.julian.QueryParameters;

public class DefaultEndpointMetadata implements EndpointMetadata {

	@Override
	public Endpoint endpoint(Class<?> javaClass, Method javaMethod, Collection<Class<?>> unhandledParameterTypes, Optional<URL> root) {
		return new MethodEndpoint(definition(JavaClass.valueOf(javaClass), JavaMethod.create(javaClass, javaMethod, unhandledParameterTypes), root), javaMethod);
	}

	private Endpoint definition(JavaClass<?> javaClass, JavaMethod javaMethod, Optional<URL> root) {
		return new Endpoint(path(javaClass, javaMethod, root, javaMethod.parameters()),
				javaMethod.httpMethod(),
				headers(javaClass, javaMethod),
				cookies(javaClass, javaMethod),
				javaMethod.parameters(),
				javaMethod.returnType());
	}

	private Headers headers(JavaClass<?> javaClass, JavaMethod javaMethod) {
		return new Headers(Stream.concat(javaClass.headers(), javaMethod.headers())
			.map(e -> new Header(e.getKey(), e.getValue()))
			.collect(toUnmodifiableList()));
	}

	private Cookies cookies(JavaClass<?> javaClass, JavaMethod javaMethod) {
		return new Cookies(Stream.concat(javaClass.cookies(), javaMethod.cookies())
				.map(e -> new Cookie(e.getKey(), e.getValue()))
				.collect(toUnmodifiableList()));
	}

	private Endpoint.Path path(JavaClass<?> javaClass, JavaMethod javaMethod, Optional<URL> root, Parameters parameters) {
		return new Endpoint.Path(path(javaClass, javaMethod, root), queryParameters(javaClass, javaMethod), parameters);
	}

	private QueryParameters queryParameters(JavaClass<?> javaClass, JavaMethod javaMethod) {
		return new QueryParameters(Stream.concat(javaClass.query(), javaMethod.query())
				.collect(groupingBy(Entry::getKey, flatMapping(e -> e.getValue().stream(), toUnmodifiableList()))));
	}

	private String path(JavaClass<?> javaClass, JavaMethod javaMethod, Optional<URL> root) {
		return Stream.concat(root.map(URL::toString).stream(), Stream.concat(javaClass.path(), javaMethod.path().stream()))
				.map(path -> path.startsWith("/") ? path.substring(1) : path)
				.collect(joining("/"));
	}
}
