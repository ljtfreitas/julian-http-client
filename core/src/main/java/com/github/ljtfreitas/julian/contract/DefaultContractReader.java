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
import java.net.URL;
import java.util.Optional;

import com.github.ljtfreitas.julian.Definition;
import com.github.ljtfreitas.julian.Endpoint;

import static java.util.stream.Collectors.toUnmodifiableList;

public class DefaultContractReader implements ContractReader {

	@Override
	public Contract read(Definition definition) {
		Endpoints endpoints = new Endpoints(definition.map(this::endpoint).collect(toUnmodifiableList()));
		return new DefaultContract(endpoints);
	}
	
	private Endpoint endpoint(Class<?> javaClass, Method javaMethod, Optional<URL> root) {
		return new DefaultEndpointMetadata(javaClass, javaMethod).endpoint(root);
	}
}
