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

package com.github.ljtfreitas.julian;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Stream;

import static com.github.ljtfreitas.julian.Message.format;
import static com.github.ljtfreitas.julian.Preconditions.check;
import static com.github.ljtfreitas.julian.Preconditions.isTrue;

public class Definition {

	private final Class<?> javaClass;
	private final URL root;

	Definition(Class<?> javaClass) {
		this(javaClass, null);
	}

	Definition(Class<?> javaClass, URL root) {
		this.javaClass = check(javaClass, Preconditions::nonNull, t -> isTrue(t, Class::isInterface, () -> format("{0} must be a interface.", javaClass)));
		this.root = root;
	}

	public <R> Stream<R> map(DefinitionFn<R> fn) {
		return methods().map(m -> fn.apply(javaClass, m, Optional.ofNullable(root)));
	}

	private Stream<Method> methods() {
		return Arrays.stream(javaClass.getMethods())
				.filter(m -> m.getDeclaringClass() != Object.class && !m.isDefault() && !Modifier.isStatic(m.getModifiers()));
	}
	
	public interface DefinitionFn<R> {
	
		R apply(Class<?> javaClass, Method javaMethod, Optional<URL> root);
	}
}
