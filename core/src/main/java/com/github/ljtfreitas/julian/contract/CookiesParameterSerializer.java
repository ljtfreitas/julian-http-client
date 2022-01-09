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

import com.github.ljtfreitas.julian.Cookie;
import com.github.ljtfreitas.julian.Cookies;
import com.github.ljtfreitas.julian.JavaType;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toUnmodifiableList;

public class CookiesParameterSerializer implements ParameterSerializer<Object, Cookies> {

	@Override
	public Optional<Cookies> serialize(String name, JavaType javaType, Object value) {
		return Optional.ofNullable(value).map(v -> serializeByJavaType(name, javaType, v));
	}

	private Cookies serializeByJavaType(String name, JavaType javaType, Object value) {
		return javaType.when(Collection.class, () -> serializeAsCollection(name, javaType, value))
				   .or(() -> javaType.when(Cookies.class, () -> serializeAsCookies(value)))
				   .or(() -> javaType.when(Map.class, () -> serializeAsMap(javaType, value)))
				   .or(() -> javaType.genericArray()
						   .map(GenericArrayType::getGenericComponentType)
						   .or(() -> javaType.array().map(Class::getComponentType))
						   .map(arrayType -> serializeAsArray(name, arrayType, value)))
				   .orElseGet(() -> serializeAsString(name, value));
	}

	private Cookies serializeAsArray(String name, Type arrayType, Object value) {
		Object[] array = (Object[]) value;
		return serializeAsCollection(name, JavaType.parameterized(Collection.class, arrayType), Arrays.asList(array));
	}
	
	private Cookies serializeAsMap(JavaType javaType, Object value) {
		return javaType.parameterized()
					   .filter(p -> JavaType.Parameterized.firstArg(p).equals(String.class))
			   		   .map(p -> serializeAsStringMap(value))
			   		   .orElseThrow(() -> new IllegalStateException("Map arguments needs to be parameterized with <String, ?>."));
	}

	@SuppressWarnings("unchecked")
	private Cookies serializeAsStringMap(Object value) {
		Map<String, ?> values = (Map<String, ?>) value;
		return new Cookies(values.entrySet().stream().map(e -> new Cookie(e.getKey(), e.getValue().toString())).collect(toUnmodifiableList()));
	}
	
	private Cookies serializeAsCollection(String name, JavaType javaType, Object value) {
		return javaType.parameterized()
					   .map(JavaType.Parameterized::firstArg)
					   .map(JavaType::valueOf)
					   .flatMap(arg -> arg.when(Cookie.class, () -> serializeAsCookieCollection(value)))
					   .orElseGet(() -> serializeAsStringCollection(name, value));
	}

	private Cookies serializeAsCookies(Object value) {
		return (Cookies) value;
	}

	@SuppressWarnings("unchecked")
	private Cookies serializeAsCookieCollection(Object value) {
		Collection<Cookie> values = (Collection<Cookie>) value;
		return new Cookies(values);
	}

	private Cookies serializeAsStringCollection(String name, Object value) {
		Collection<?> values = (Collection<?>) value;
		return new Cookies(values.stream().map(v -> new Cookie(name, v.toString())).collect(Collectors.toUnmodifiableList()));
	}

	private Cookies serializeAsString(String name, Object value) {
		return Cookies.create(new Cookie(name, value.toString()));
	}
}
