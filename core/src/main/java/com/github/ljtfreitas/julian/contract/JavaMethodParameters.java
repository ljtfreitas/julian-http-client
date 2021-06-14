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

import com.github.ljtfreitas.julian.EndpointDefinition;
import com.github.ljtfreitas.julian.EndpointDefinition.Parameter;
import com.github.ljtfreitas.julian.EndpointDefinition.Parameters;
import com.github.ljtfreitas.julian.JavaType;
import com.github.ljtfreitas.julian.JavaType.Parameterized;
import com.github.ljtfreitas.julian.Preconditions.Precondition;

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
		return check(parameters, justOneBodyParameter(), allowedCallbackParameters())
				.values()
				.stream()
				.flatMap(List::stream)
				.collect(toUnmodifiableList());
	}

	private <T> Precondition<Map<Class<? extends Parameter>, List<Parameter>>, Map<Class<? extends Parameter>, List<Parameter>>> allowedCallbackParameters() {
		Predicate<JavaType> isException = jt -> jt.is(Exception.class);
		Predicate<JavaType> isThrowable = jt -> jt.is(Throwable.class);

		return p -> state(p, m -> {
			Collection<Parameter> parameters = m.getOrDefault(EndpointDefinition.CallbackParameter.class, emptyList());
			return parameters.isEmpty() 
				|| parameters.stream().anyMatch(c -> c.javaType().when(Consumer.class, () -> c.javaType().parameterized().map(Parameterized::firstArg).map(JavaType::valueOf).filter(not(isException)).isPresent())
				 						   .or(() -> c.javaType().when(BiConsumer.class, () -> c.javaType().parameterized().map(t -> t.getActualTypeArguments()[1]).map(JavaType::valueOf).filter(isThrowable).isPresent()))
				 						   .orElse(false));
		}, () -> format("Method {0} is invalid; check @Callback parameters (just Consumer<SomeType>, Consumer<Throwable>, or BiConsumer<SomeType, Throwable> are allowed).", javaMethod));
	}

	private <T> Precondition<Map<Class<? extends Parameter>, List<Parameter>>, Map<Class<? extends Parameter>, List<Parameter>>> justOneBodyParameter() {
		return p -> state(p, m -> m.getOrDefault(EndpointDefinition.BodyParameter.class, emptyList()).size() <= 1, 
				() -> format("Method {0} is invalid; it's allowed just one parameter annotated with @BodyParameter.", javaMethod));
	}

}
