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

package com.github.ljtfreitas.julian.http.resilience4j;

import com.github.ljtfreitas.julian.Promise;
import com.github.ljtfreitas.julian.http.HTTPRequest;
import com.github.ljtfreitas.julian.http.HTTPRequestInterceptor;
import com.github.ljtfreitas.julian.http.HTTPResponse;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;

import java.util.function.Predicate;

public class CircuitBreakerHTTPRequestInterceptor implements HTTPRequestInterceptor {

    private final CircuitBreaker circuitBreaker;
    private final Predicate<HTTPResponse<?>> predicate;

    public CircuitBreakerHTTPRequestInterceptor(CircuitBreaker circuitBreaker) {
        this(circuitBreaker, r -> r.status().isSuccess());
    }

    public CircuitBreakerHTTPRequestInterceptor(CircuitBreaker circuitBreaker, Predicate<HTTPResponse<?>> predicate) {
        this.circuitBreaker = circuitBreaker;
        this.predicate = predicate;
    }

    @Override
    public <T> Promise<HTTPRequest<T>> intercepts(Promise<HTTPRequest<T>> request) {
        return request.then(r -> new CircuitBreakerHTTPRequest<>(circuitBreaker, predicate, r));
    }

}
