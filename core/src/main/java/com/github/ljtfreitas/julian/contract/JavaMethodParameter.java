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
import java.lang.reflect.Parameter;

import com.github.ljtfreitas.julian.JavaType;

import static com.github.ljtfreitas.julian.Preconditions.isTrue;
import static com.github.ljtfreitas.julian.Preconditions.nonNull;

class JavaMethodParameter {

	private final int position;
	private final String name;
	private final JavaType returnType;
	private final ParameterDefinitionReader reader;

	private JavaMethodParameter(int position, String name, JavaType returnType, ParameterDefinitionReader reader) {
		this.position = position;
		this.name = name;
		this.returnType = returnType;
		this.reader = reader;
	}

	com.github.ljtfreitas.julian.EndpointDefinition.Parameter kind() {
		return reader.parameter(position, name, returnType);
	}

	static JavaMethodParameter create(int position, Parameter javaParameter, Class<?> declaredOn) {
		Scannotation scannotation = new Scannotation(nonNull(javaParameter));

		Annotation definition = isTrue(scannotation.meta(ParameterDefinition.class).toArray(Annotation[]::new),
				a -> a.length == 1, () -> "Method parameter must have a @ParameterDefinition annotation!")[0];

		JavaType returnType = JavaType.valueOf(declaredOn, javaParameter.getParameterizedType());

		ParameterDefinitionReader reader = new ParameterDefinitionReader(definition);
		
		String name = reader.name().orElseGet(javaParameter::getName);

		return new JavaMethodParameter(position, name, returnType, reader);
	}
}
