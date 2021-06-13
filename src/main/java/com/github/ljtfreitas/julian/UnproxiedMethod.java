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

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.util.Arrays;

import static com.github.ljtfreitas.julian.Message.format;

class UnproxiedMethod {

	private final Object target;
	private final Method method;

	UnproxiedMethod(Object target, Method method) {
		this.target = target;
		this.method = method;
	}

	Object call(Object[] args) {
		return Except.run(MethodHandles::lookup)
                .map(l -> l.findSpecial(method.getDeclaringClass(), method.getName(), methodTypeOf(method), method.getDeclaringClass()))
                .map(h -> h.bindTo(target))
                .map(h -> h.invokeWithArguments(args))
                .prop(e -> new MethodInvocationException(format("Method {0} failed with args: {1}", method, Arrays.toString(args)), e));
	}

	private MethodType methodTypeOf(Method method) {
        return MethodType.methodType(method.getReturnType(), method.getParameterTypes());
    }
}
