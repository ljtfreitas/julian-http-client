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

import java.lang.reflect.GenericArrayType;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.github.ljtfreitas.julian.JavaType;
import com.github.ljtfreitas.julian.QueryString;

import static java.util.stream.Collectors.flatMapping;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toUnmodifiableList;

public class QueryStringSerializer implements ParameterSerializer<Object, QueryString> {

	@Override
	public Optional<QueryString> serialize(String name, JavaType javaType, Object value) {
		return Optional.ofNullable(value).map(v -> serializeByJavaType(name, javaType, v));
	}

	private QueryString serializeByJavaType(String name, JavaType javaType, Object value) {
		return javaType.when(Collection.class, () -> serializeAsCollection(name, value))
					   .or(() -> javaType.when(QueryString.class, () -> serializeAsQueryString(value)))
					   .or(() -> javaType.when(Map.class, () -> serializeAsMap(javaType, value)))
					   .or(() -> javaType.genericArray()
							   			 .map(GenericArrayType::getGenericComponentType)
							   			 .or(javaType::array)
					   					 .map(arrayType -> serializeAsArray(name, value)))
					   .orElseGet(() -> serializeAsString(name, value));
	}

	private QueryString serializeAsMap(JavaType javaType, Object value) {
		return javaType.parameterized()
				   .filter(p -> JavaType.Parameterized.firstArg(p).equals(String.class))
				   .map(p -> JavaType.valueOf(p.getActualTypeArguments()[1]))
				   .map(valueType -> valueType.when(Collection.class, () -> serializeAsMultiMap(value))
					   						  .orElseGet(() -> serializeAsStringMap(value)))
		   		   .orElseThrow(() -> new IllegalStateException("Map arguments needs to be parameterized with <String, ?>"));
	}

	@SuppressWarnings("unchecked")
	private QueryString serializeAsMultiMap(Object value) {
		Map<String, ? extends Collection<?>> values = (Map<String, ? extends Collection<?>>) value;

		return new QueryString(values.entrySet().stream()
				.map(e -> Map.entry(e.getKey(), e.getValue().stream()
						.map(Object::toString)
						.collect(toUnmodifiableList())))
				.collect(groupingBy(Map.Entry::getKey, flatMapping(e -> e.getValue().stream(), toUnmodifiableList()))));
	}

	@SuppressWarnings("unchecked")
	private QueryString serializeAsStringMap(Object value) {
		Map<String, ?> values = (Map<String, ?>) value;

		return new QueryString(values.entrySet().stream()
				.map(e -> Map.entry(e.getKey(), e.getValue().toString()))
				.collect(groupingBy(Map.Entry::getKey, Collectors.mapping(Map.Entry::getValue, toUnmodifiableList()))));
	}

	private QueryString serializeAsArray(String name, Object value) {
		Object[] array = (Object[]) value;
		return serializeAsCollection(name, Arrays.asList(array));
	}

	private QueryString serializeAsQueryString(Object value) {
		return (QueryString) value;
	}

	private QueryString serializeAsString(String name, Object value) {
		return QueryString.create(name, value.toString());
	}

	private QueryString serializeAsCollection(String name, Object value) {
		Collection<?> values = (Collection<?>) value;
		return new QueryString(Map.of(name, values.stream().map(Object::toString).collect(toUnmodifiableList())));
	}

}
