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
import java.lang.reflect.AnnotatedElement;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Stream;

class Scannotation {

	private AnnotatedElement element;

	Scannotation(AnnotatedElement element) {
		this.element = element;
	}

	<A extends Annotation> Stream<A> scan(Class<A> annotation) {
		return Stream.concat(Arrays.stream(element.getAnnotationsByType(annotation)),
							 Arrays.stream(element.getAnnotations()).flatMap(a -> Arrays.stream(a.annotationType().getAnnotationsByType(annotation))));
	}

	<A extends Annotation> Optional<A> find(Class<A> annotation) {
		return Optional.ofNullable(element.getAnnotation(annotation));
	}

	<A extends Annotation> Stream<Annotation> meta(Class<A> annotation) {
		return Arrays.stream(element.getAnnotations()).filter(a -> a.annotationType().isAnnotationPresent(annotation));
	}

}
