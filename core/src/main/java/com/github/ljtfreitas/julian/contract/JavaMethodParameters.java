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
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;

import com.github.ljtfreitas.julian.Endpoint;
import com.github.ljtfreitas.julian.Endpoint.Parameter;
import com.github.ljtfreitas.julian.Endpoint.Parameters;
import com.github.ljtfreitas.julian.JavaType;
import com.github.ljtfreitas.julian.JavaType.Parameterized;
import com.github.ljtfreitas.julian.Preconditions.Precondition;
import com.github.ljtfreitas.julian.Promise;

import static com.github.ljtfreitas.julian.Message.format;
import static com.github.ljtfreitas.julian.Preconditions.check;
import static com.github.ljtfreitas.julian.Preconditions.state;
import static java.util.Collections.emptyList;
import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toUnmodifiableList;
import static java.util.stream.IntStream.range;

class JavaMethodParameters {

	private final Class<?> declaredOn;
	private final Method javaMethod;

	JavaMethodParameters(Class<?> declaredOn, Method javaMethod) {
		this.declaredOn = declaredOn;
		this.javaMethod = javaMethod;
	}

	Parameters read() {
		 Map<Class<? extends Parameter>, List<Parameter>> parameters = range(0, javaMethod.getParameterCount())
				 .mapToObj(i -> JavaMethodParameter.create(i, javaMethod.getParameters()[i], declaredOn))
				 .map(JavaMethodParameter::kind)
				 .collect(toUnmodifiableList()).stream()
				 .collect(groupingBy(Parameter::getClass));

		 return new Parameters(before(parameters));
	}

	private Collection<Parameter> before(Map<Class<? extends Parameter>, List<Parameter>> parameters) {
		return check(parameters, justOneBodyParameter())
				.values()
				.stream()
				.flatMap(List::stream)
				.collect(toUnmodifiableList());
	}

	private <T> Precondition<Map<Class<? extends Parameter>, List<Parameter>>, Map<Class<? extends Parameter>, List<Parameter>>> justOneBodyParameter() {
		return p -> state(p, m -> m.getOrDefault(Endpoint.BodyParameter.class, emptyList()).size() <= 1,
				() -> format("Method {0} is invalid; it's allowed just one parameter annotated with @BodyParameter.", javaMethod));
	}

}
