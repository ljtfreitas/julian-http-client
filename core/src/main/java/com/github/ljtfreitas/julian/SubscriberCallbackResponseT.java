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
import com.github.ljtfreitas.julian.JavaType.Parameterized;

import java.lang.reflect.Type;
import java.util.Optional;
import java.util.concurrent.Flow.Publisher;
import java.util.concurrent.Flow.Subscriber;
import java.util.function.Predicate;

public class SubscriberCallbackResponseT<T> implements ResponseT<Publisher<T>, Void> {

    private static final SubscriberCallbackResponseT<Object> SINGLE_INSTANCE = new SubscriberCallbackResponseT<>();

    public static SubscriberCallbackResponseT<Object> get() {
        return SINGLE_INSTANCE;
    }

    @Override
    public <A> ResponseFn<A, Void> bind(Endpoint endpoint, ResponseFn<A, Publisher<T>> fn) {
        return new ResponseFn<>() {

            @Override
            public Void join(RequestIO<A> request, Arguments arguments) {
                fn.run(request, arguments)
                        .then(publisher -> {
                            subscriber(endpoint.parameters(), arguments).ifPresent(publisher::subscribe);
                            return publisher;
                        });

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
                return null;
            }
        };
    }

    @Override
    public JavaType adapted(Endpoint endpoint) {
        return argument(endpoint, this::subscriber)
                .map(JavaType::valueOf)
                .map(javaType -> JavaType.parameterized(Publisher.class, javaType.get()))
                .orElseGet(() -> JavaType.parameterized(Publisher.class, Object.class));
    }

    private Optional<Type> argument(Endpoint endpoint, Predicate<CallbackParameter> p) {
        return endpoint.parameters().callbacks()
                .filter(p)
                .findFirst()
                .flatMap(c -> c.javaType().parameterized().map(Parameterized::firstArg));
    }

    @Override
    public boolean test(Endpoint endpoint) {
        return endpoint.returnType().equals(JavaType.none())
            && endpoint.parameters().callbacks().anyMatch(this::subscriber);
    }

    private boolean subscriber(CallbackParameter callback) {
        JavaType javaType = callback.javaType();
        return javaType.compatible(Subscriber.class) && javaType.parameterized().isPresent();
    }
}
