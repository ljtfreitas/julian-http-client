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

package com.github.ljtfreitas.julian;

import com.github.ljtfreitas.julian.Endpoint.CallbackParameter;
import com.github.ljtfreitas.julian.Endpoint.Parameters;

import java.lang.reflect.ParameterizedType;
import java.util.Optional;
import java.util.function.Predicate;

public class PromiseSubscriberCallbackResponseT<T> implements ResponseT<Promise<T>, Void> {

    private static final PromiseSubscriberCallbackResponseT<Object> SINGLE_INSTANCE = new PromiseSubscriberCallbackResponseT<>();

    public static PromiseSubscriberCallbackResponseT<Object> get() {
        return SINGLE_INSTANCE;
    }

    @Override
    public <A> ResponseFn<A, Void> bind(Endpoint endpoint, ResponseFn<A, Promise<T>> fn) {
        return new ResponseFn<>() {

            @Override
            public Void join(Promise<? extends Response<A>> response, Arguments arguments) {
                subscriber(endpoint.parameters(), arguments).ifPresent(subscriber -> fn.join(response, arguments).subscribe(subscriber));
                return null;
            }

            @SuppressWarnings("unchecked")
            private Optional<Subscriber<T>> subscriber(Parameters parameters, Arguments arguments) {
                return parameters.callbacks()
                        .filter(c -> c.javaType().compatible(Subscriber.class))
                        .findFirst()
                        .flatMap(c -> arguments.of(c.position()))
                        .map(arg -> (Subscriber<T>) arg);
            }

            @Override
            public JavaType returnType() {
                return fn.returnType();
            }
        };
    }

    @Override
    public JavaType adapted(Endpoint endpoint) {
        return argument(endpoint, this::subscriber)
                .map(parameterized -> JavaType.parameterized(Promise.class, parameterized.getActualTypeArguments()[0]))
                .orElseGet(() -> JavaType.parameterized(Promise.class, Object.class));
    }

    private Optional<ParameterizedType> argument(Endpoint endpoint, Predicate<CallbackParameter> p) {
        return endpoint.parameters().callbacks()
                .filter(p)
                .findFirst()
                .flatMap(c -> c.javaType().parameterized());
    }

    @Override
    public boolean test(Endpoint endpoint) {
        return endpoint.returnType().equals(JavaType.none())
            && endpoint.parameters().callbacks().anyMatch(this::subscriber);
    }

    private boolean subscriber(CallbackParameter callback) {
        JavaType javaType = callback.javaType();
        return javaType.compatible(Subscriber.class)
            && javaType.parameterized().isPresent();
    }
}
