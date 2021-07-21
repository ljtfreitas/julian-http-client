/*
 * Copyright (C) 2021 Tiago de Freitas Lima
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */

package com.github.ljtfreitas.julian.http;

import java.util.Collection;
import java.util.Collections;

import com.github.ljtfreitas.julian.Promise;

import static java.util.Collections.emptyList;

public class DefaultHTTPRequestInterceptor implements HTTPRequestInterceptor {

    private final Collection<HTTPRequestInterceptor> interceptors;

    public DefaultHTTPRequestInterceptor() {
        this(emptyList());
    }

    public DefaultHTTPRequestInterceptor(Collection<HTTPRequestInterceptor> interceptors) {
        this.interceptors = interceptors;
    }

    @Override
    public <T> Promise<HTTPRequest<T>> intercepts(Promise<HTTPRequest<T>> request) {
        return interceptors.stream().reduce(request, (r, i) -> i.intercepts(r), (a, b) -> b);
    }
}
