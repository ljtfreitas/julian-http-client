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
import com.github.ljtfreitas.julian.QueryParameters;
import com.github.ljtfreitas.julian.Content;

import static java.util.stream.Collectors.flatMapping;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toUnmodifiableList;

public class QueryParameterSerializer implements ParameterSerializer<Object, QueryParameters> {

	@Override
	public Optional<QueryParameters> serialize(String name, JavaType javaType, Object value) {
		return Optional.ofNullable(value).map(v -> serializeByJavaType(name, javaType, v));
	}

	private QueryParameters serializeByJavaType(String name, JavaType javaType, Object value) {
		return javaType.when(Collection.class, () -> serializeAsCollection(name, value))
					   .or(() -> javaType.when(QueryParameters.class, () -> serializeAsQueryParameters(value)))
					   .or(() -> javaType.when(Map.class, () -> serializeAsMap(javaType, value)))
					   .or(() -> javaType.genericArray()
							   			 .map(GenericArrayType::getGenericComponentType)
							   			 .or(() -> javaType.array().map(Class::getComponentType))
					   					 .map(arrayType -> serializeAsArray(name, value)))
					   .or(() -> javaType.when(Content.class, () -> serializeAsContent(name, value)))
					   .orElseGet(() -> serializeAsString(name, value));
	}

	private QueryParameters serializeAsMap(JavaType javaType, Object value) {
		return javaType.parameterized()
				   .filter(p -> JavaType.Parameterized.firstArg(p).equals(String.class))
				   .map(p -> JavaType.valueOf(p.getActualTypeArguments()[1]))
				   .map(valueType -> valueType.when(Collection.class, () -> serializeAsMultiMap(value))
					   						  .orElseGet(() -> serializeAsStringMap(value)))
		   		   .orElseThrow(() -> new IllegalStateException("Map arguments needs to be parameterized with <String, ?>"));
	}

	@SuppressWarnings("unchecked")
	private QueryParameters serializeAsMultiMap(Object value) {
		Map<String, ? extends Collection<?>> values = (Map<String, ? extends Collection<?>>) value;

		return new QueryParameters(values.entrySet().stream()
				.map(e -> Map.entry(e.getKey(), e.getValue().stream()
						.map(Object::toString)
						.collect(toUnmodifiableList())))
				.collect(groupingBy(Map.Entry::getKey, flatMapping(e -> e.getValue().stream(), toUnmodifiableList()))));
	}

	@SuppressWarnings("unchecked")
	private QueryParameters serializeAsStringMap(Object value) {
		Map<String, ?> values = (Map<String, ?>) value;

		return new QueryParameters(values.entrySet().stream()
				.map(e -> Map.entry(e.getKey(), e.getValue().toString()))
				.collect(groupingBy(Map.Entry::getKey, Collectors.mapping(Map.Entry::getValue, toUnmodifiableList()))));
	}

	private QueryParameters serializeAsArray(String name, Object value) {
		Object[] array = (Object[]) value;
		return serializeAsCollection(name, Arrays.asList(array));
	}

	private QueryParameters serializeAsQueryParameters(Object value) {
		return (QueryParameters) value;
	}

	private QueryParameters serializeAsContent(String name, Object value) {
		Content content = (Content) value;
		return serializeAsString(name, content.show());
	}

	private QueryParameters serializeAsString(String name, Object value) {
		return QueryParameters.create(name, value.toString());
	}

	private QueryParameters serializeAsCollection(String name, Object value) {
		Collection<?> values = (Collection<?>) value;
		return new QueryParameters(Map.of(name, values.stream().map(Object::toString).collect(toUnmodifiableList())));
	}

}
