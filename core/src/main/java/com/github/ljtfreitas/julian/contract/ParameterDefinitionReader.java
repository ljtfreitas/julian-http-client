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

import java.lang.annotation.Annotation;
import java.util.Optional;
import java.util.function.Function;

import com.github.ljtfreitas.julian.Endpoint;
import com.github.ljtfreitas.julian.Endpoint.Parameter;
import com.github.ljtfreitas.julian.Except;
import com.github.ljtfreitas.julian.JavaType;

import static java.util.function.Predicate.not;

class ParameterDefinitionReader {

	private final Annotation definition;

	ParameterDefinitionReader(Annotation definition) {
		this.definition = definition;
	}

	Optional<String> name() {
		return queryParameterName().or(this::headerName).or(this::cookieName).or(this::pathName)
				.filter(not(String::isEmpty));
	}

	private Optional<String> pathName() {
		return annotationType(Path.class, Path::name)
				.filter(not(String::isEmpty))
				.or(() -> annotationType(Path.class, Path::value)
						.filter(not(String::isEmpty)));
	}

	private Optional<String> cookieName() {
		return annotationType(Cookie.class, Cookie::name)
				.filter(not(String::isEmpty))
				.or(() -> annotationType(Cookie.class, Cookie::value))
						.filter(not(String::isEmpty));
	}

	private Optional<String> headerName() {
		return annotationType(Header.class, Header::name)
				.filter(not(String::isEmpty))
				.or(() -> annotationType(Header.class, Header::value)
						.filter(a -> a.length > 0)
						.map(a -> a[0])
						.filter(not(String::isEmpty)));
	}

	private Optional<String> queryParameterName() {
		return annotationType(QueryParameter.class, QueryParameter::name)
				.filter(not(String::isEmpty))
				.or(() -> annotationType(QueryParameter.class, QueryParameter::value)
						.filter(a -> a.length > 0)
						.map(a -> a[0])
						.filter(not(String::isEmpty)));
	}

	Parameter parameter(int position, String name, JavaType returnType) {
		return annotationType(QueryParameter.class, a -> Endpoint.Parameter.query(position, name, returnType,
					instantiate(a.serializer()).unsafe(), a.defaultValue()))
				.or(() -> annotationType(Header.class, a -> Endpoint.Parameter.header(position, name, returnType,
					instantiate(a.serializer()).unsafe(), a.defaultValue())))
				.or(() -> annotationType(Cookie.class, a -> Endpoint.Parameter.cookie(position, name, returnType,
					instantiate(a.serializer()).unsafe(), a.defaultValue())))
				.or(() -> annotationType(Body.class, a -> Endpoint.Parameter.body(position, name, returnType, a.value())))
				.or(() -> annotationType(Callback.class, a -> Endpoint.Parameter.callback(position, name, returnType)))
				.or(() -> annotationType(Path.class, a -> Endpoint.Parameter.path(position, name, returnType,
					instantiate(a.serializer()).recover(DefaultParameterSerializer::new), a.defaultValue())))
				.orElseThrow();
	}

	private <T> Except<ParameterSerializer<? super Object, T>> instantiate(Class<? extends ParameterSerializer<? super Object, T>> serializerClassType) {
		return Except.run(() -> serializerClassType.getDeclaredConstructor().newInstance());
	}

	private <A extends Annotation, R> Optional<R> annotationType(Class<A> annotationType, Function<A, R> fn) {
		return annotationType.isInstance(definition) ? Optional.of(fn.apply(annotationType.cast(definition))) : Optional.empty();
	}
}
