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
import java.util.function.Predicate;

import com.github.ljtfreitas.julian.EndpointDefinition.Parameters;
import com.github.ljtfreitas.julian.EndpointDefinition.Path;

public class MethodEndpoint implements Endpoint, Predicate<Method> {

	private final Endpoint endpoint;
	private final Method source;

	public MethodEndpoint(Endpoint endpoint, Method source) {
		this.endpoint = endpoint;
		this.source = source;
	}

	public Path path() {
		return endpoint.path();
	}

	public String method() {
		return endpoint.method();
	}

	public Headers headers() {
		return endpoint.headers();
	}

	public Cookies cookies() {
		return endpoint.cookies();
	}

	public Parameters parameters() {
		return endpoint.parameters();
	}

	public JavaType returnType() {
		return endpoint.returnType();
	}
	
	@Override
	public boolean test(Method t) {
		return source.equals(t);
	}
}
