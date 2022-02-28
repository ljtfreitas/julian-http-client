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

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Stream;

import com.github.ljtfreitas.julian.Endpoint.Parameters;
import com.github.ljtfreitas.julian.Except;
import com.github.ljtfreitas.julian.JavaType;

import static com.github.ljtfreitas.julian.Message.format;
import static com.github.ljtfreitas.julian.Preconditions.nonNull;
import static com.github.ljtfreitas.julian.Preconditions.state;
import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.toUnmodifiableList;

class JavaMethod {

	private final Method source;
	private final Optional<String> path;
	private final String httpMethod;
	private final Stream<Header> headers;
	private final Stream<Cookie> cookies;
	private final Stream<QueryParameter> queryParameters;
	private final Parameters parameters;
	private final JavaType returnType;

	private JavaMethod(Method source, Optional<String> path, String httpMethod, Stream<Header> headers,
					   Stream<Cookie> cookies, Stream<QueryParameter> queryParameters, Parameters parameters, JavaType returnType) {
		this.source = source;
		this.path = path;
		this.httpMethod = httpMethod;
		this.headers = headers;
		this.cookies = cookies;
		this.queryParameters = queryParameters;
		this.parameters = parameters;
		this.returnType = returnType;
	}

	Method source() {
		return source;
	}

	Optional<String> path() {
		return path.filter(not(String::isEmpty));
	}

	Stream<Entry<String, Collection<String>>> headers() {
		return headers.map(h -> Map.entry(h.name(), Arrays.asList(h.value())));
	}

	Stream<Entry<String, String>> cookies() {
		return cookies.map(c -> Map.entry(c.name(), c.value()));
	}

	Stream<Entry<String, Collection<String>>> query() {
		return queryParameters.map(q -> Map.entry(q.name(), Arrays.asList(q.value())));
	}

	Parameters parameters() {
		return parameters;
	}

	String httpMethod() {
		return httpMethod;
	}

	JavaType returnType() {
		return returnType;
	}

	static JavaMethod create(Class<?> declaredOn, Method javaMethod, Collection<Class<?>> unhandledParameterTypes) {
		nonNull(declaredOn);
		nonNull(javaMethod);

		Scannotation scannotation = new Scannotation(javaMethod);

		HTTPMethodDefinition httpMethodDefinition = scannotation.find(HTTPMethodDefinition.class)
				.or(() -> state(scannotation.meta(HTTPMethodDefinition.class).collect(toUnmodifiableList()), s -> s.size() <= 1, 
								() -> "Method {0} is invalid; it's allowed just one HTTP method annotation.")
							.stream()
							.findFirst()
							.map(m -> m.annotationType().getAnnotation(HTTPMethodDefinition.class)))
				.orElseThrow(() -> new IllegalStateException(
						format("Method {0} must be annotated with some HTTP method.", javaMethod)));

        String httpMethod = httpMethodDefinition.value();

		Optional<String> path = scannotation.find(Path.class)
				.map(Path::value)
				.or(() -> scannotation.meta(HTTPMethodDefinition.class)
								.findFirst()
								.map(a -> Except.run(() -> a.annotationType().getMethod("value"))
												.map(m -> m.invoke(a).toString())
												.unsafe()));

        Stream<Header> headers = scannotation.scan(Header.class);
        Stream<Cookie> cookies = scannotation.scan(Cookie.class);
        Stream<QueryParameter> queryParameters = scannotation.scan(QueryParameter.class);

		Parameters parameters = new JavaMethodParameters(declaredOn, javaMethod).read(unhandledParameterTypes);

		JavaType returnType = JavaType.valueOf(declaredOn, javaMethod.getGenericReturnType());

        return new JavaMethod(javaMethod, path, httpMethod, headers, cookies, queryParameters, parameters, returnType);
    }
}
