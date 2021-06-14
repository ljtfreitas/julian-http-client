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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

import com.github.ljtfreitas.julian.contract.Contract;

class DefaultInvocationHandler implements InvocationHandler {

	private final Contract contract;
	private final RequestRunner requestRunner;

	DefaultInvocationHandler(Contract contract, RequestRunner requestRunner) {
		this.contract = contract;
		this.requestRunner = requestRunner;
	}

	@Override
	public Object invoke(Object proxy, Method method, Object[] args) {
		return contract.endpoints()
				.select(method)
				.map(endpoint -> requestRunner.run(endpoint, Arguments.create(args)))
				.orElseGet(() -> isObjectMethod(method) ? new ObjectMethod(this, method).call(args) : new UnproxiedMethod(proxy, method).call(args));
	}

	private boolean isObjectMethod(Method method) {
		return method.getDeclaringClass().equals(Object.class);
	}
}
