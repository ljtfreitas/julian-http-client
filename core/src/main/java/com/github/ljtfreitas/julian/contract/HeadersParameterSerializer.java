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

import static java.util.stream.Collectors.toUnmodifiableList;

import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

import com.github.ljtfreitas.julian.Header;
import com.github.ljtfreitas.julian.Headers;
import com.github.ljtfreitas.julian.JavaType;
import com.github.ljtfreitas.julian.Content;

public class HeadersParameterSerializer implements ParameterSerializer<Object, Headers> {

	@Override
	public Optional<Headers> serialize(String name, JavaType javaType, Object value) {
		return Optional.ofNullable(value).map(v -> serializeByJavaType(name, javaType, v));
	}

	private Headers serializeByJavaType(String name, JavaType javaType, Object value) {
		return javaType.when(Collection.class, () -> serializeAsCollection(name, javaType, value))
						.or(() -> javaType.when(Headers.class, () -> serializeAsHeaders(value)))
					   	.or(() -> javaType.when(Map.class, () -> serializeAsMap(javaType, value)))
					   	.or(() -> javaType.genericArray()
									.map(GenericArrayType::getGenericComponentType)
									.or(() -> javaType.array().map(Class::getComponentType))
									.map(arrayType -> serializeAsArray(name, arrayType, value)))
					   	.or(() -> javaType.when(Content.class, () -> serializeAsContent(name, value)))
					   	.orElseGet(() -> serializeAsString(name, value));
	}

	private Headers serializeAsMap(JavaType javaType, Object value) {
		return javaType.parameterized()
					   .filter(p -> JavaType.Parameterized.firstArg(p).equals(String.class))
			   		   .map(p -> serializeAsStringMap(value))
			   		   .orElseThrow(() -> new IllegalStateException("Map arguments needs to be parameterized with <String, ?>."));
	}

	@SuppressWarnings("unchecked")
	private Headers serializeAsStringMap(Object value) {
		Map<String, ?> values = (Map<String, ?>) value;
		return new Headers(values.entrySet().stream().map(e -> new Header(e.getKey(), e.getValue().toString())).collect(toUnmodifiableList()));
	}

	private Headers serializeAsArray(String name, Type arrayType, Object value) {
		Object[] array = (Object[]) value;
		return serializeAsCollection(name, JavaType.parameterized(Collection.class, arrayType), Arrays.asList(array));
	}

	private Headers serializeAsHeaders(Object value) {
		return (Headers) value;
	}

	private Headers serializeAsContent(String name, Object value) {
		Content content = (Content) value;
		return serializeAsString(name, content.show());
	}

	private Headers serializeAsString(String name, Object value) {
		return Headers.create(new Header(name, value.toString()));
	}

	private Headers serializeAsCollection(String name, JavaType javaType, Object value) {
		return javaType.parameterized()
					   .map(JavaType.Parameterized::firstArg)
					   .map(JavaType::valueOf)
					   .flatMap(arg -> arg.when(Header.class, () -> serializeAsHeaderCollection(value)))
					   .orElseGet(() -> serializeAsStringCollection(name, value));
	}

	private Headers serializeAsStringCollection(String name, Object value) {
		Collection<?> values = (Collection<?>) value;
		return Headers.create(new Header(name, values.stream().map(Object::toString).collect(toUnmodifiableList())));
	}

	@SuppressWarnings("unchecked")
	private Headers serializeAsHeaderCollection(Object value) {
		Collection<Header> values = (Collection<Header>) value;
		return new Headers(values);
	}
}
