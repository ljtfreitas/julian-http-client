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

import com.github.ljtfreitas.julian.Form;
import com.github.ljtfreitas.julian.JavaType;

import java.lang.reflect.GenericArrayType;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.stream.StreamSupport;

import static java.util.stream.Collectors.flatMapping;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toUnmodifiableList;

public class FormSerializer implements ParameterSerializer<Object, Form> {

	@Override
	public Optional<Form> serialize(String name, JavaType javaType, Object value) {
		return Optional.ofNullable(value).map(v -> serializeByJavaType(name, javaType, v));
	}

	private Form serializeByJavaType(String name, JavaType javaType, Object value) {
		return javaType.when(Iterable.class, () -> serializeAsIterable(name, value))
				.or(() -> javaType.when(Form.class, () -> serializeAsForm(value)))
				.or(() -> javaType.when(Map.class, () -> serializeAsMap(javaType, value)))
				.or(() -> javaType.genericArray()
						.map(GenericArrayType::getGenericComponentType)
						.or(() -> javaType.array().map(Class::getComponentType))
						.map(arrayType -> serializeAsArray(name, value)))
				.orElseGet(() -> serializeAsString(name, value));
	}

	private Form serializeAsMap(JavaType javaType, Object value) {
		return javaType.parameterized()
				.filter(p -> JavaType.Parameterized.firstArg(p).equals(String.class))
				.map(p -> JavaType.valueOf(p.getActualTypeArguments()[1]))
				.map(valueType -> valueType.when(Iterable.class, () -> serializeAsMultiMap(value))
						.orElseGet(() -> serializeAsStringMap(value)))
				.orElseThrow(() -> new IllegalStateException("Map arguments needs to be parameterized with <String, ?>"));
	}

	@SuppressWarnings("unchecked")
	private Form serializeAsMultiMap(Object value) {
		Map<String, ? extends Iterable<?>> values = (Map<String, ? extends Collection<?>>) value;

		return new Form(values.entrySet().stream()
				.map(e -> Map.entry(e.getKey(), StreamSupport.stream(e.getValue().spliterator(), false)
						.map(Object::toString)
						.collect(toUnmodifiableList())))
				.collect(groupingBy(Map.Entry::getKey, flatMapping(e -> e.getValue().stream(), toUnmodifiableList()))));
	}

	@SuppressWarnings("unchecked")
	private Form serializeAsStringMap(Object value) {
		Map<String, ?> values = (Map<String, ?>) value;

		return new Form(values.entrySet().stream()
				.map(e -> Map.entry(e.getKey(), e.getValue().toString()))
				.collect(groupingBy(Map.Entry::getKey, mapping(Map.Entry::getValue, toUnmodifiableList()))));
	}

	private Form serializeAsForm(Object value) {
		return (Form) value;
	}

	private Form serializeAsString(String name, Object value) {
		return Form.create(name, value.toString());
	}

	private Form serializeAsIterable(String name, Object value) {
		Iterable<?> values = (Iterable<?>) value;

		return Form.create(StreamSupport.stream(values.spliterator(), false)
				.map(Object::toString)
				.collect(groupingBy(e -> name)));
	}

	private Form serializeAsArray(String name, Object value) {
		Object[] array = (Object[]) value;
		return serializeAsIterable(name, Arrays.asList(array));
	}
}