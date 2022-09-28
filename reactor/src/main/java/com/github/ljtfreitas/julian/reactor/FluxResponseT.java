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

package com.github.ljtfreitas.julian.reactor;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;

import com.github.ljtfreitas.julian.Arguments;
import com.github.ljtfreitas.julian.Endpoint;
import com.github.ljtfreitas.julian.JavaType;
import com.github.ljtfreitas.julian.Kind;
import com.github.ljtfreitas.julian.Promise;
import com.github.ljtfreitas.julian.Response;
import com.github.ljtfreitas.julian.ResponseFn;
import com.github.ljtfreitas.julian.ResponseT;

import static java.util.function.Function.identity;

public class FluxResponseT implements ResponseT<Collection<Object>, Flux<Object>> {

    private static final FluxResponseT SINGLE_INSTANCE = new FluxResponseT();

    @Override
    public <A> ResponseFn<A, Flux<Object>> bind(Endpoint endpoint, ResponseFn<A, Collection<Object>> next) {
        return new ResponseFn<>() {

            @Override
            public Flux<Object> join(Promise<? extends Response<A, ? extends Throwable>> response, Arguments arguments) {
                Promise<Collection<Object>> promise = next.run(response, arguments);

                return promise.cast(new Kind<MonoPromise<Collection<Object>>>() {})
                        .map(MonoPromise::mono)
                        .orElseGet(() -> Mono.fromFuture(promise.future()))
                        .flatMapIterable(identity());
            }

            @Override
            public JavaType returnType() {
                return next.returnType();
            }
        };
    }

    @Override
    public JavaType adapted(Endpoint endpoint) {
        return JavaType.parameterized(Collection.class, endpoint.returnType().parameterized().map(JavaType.Parameterized::firstArg).orElse(Object.class));
    }

    @Override
    public boolean test(Endpoint endpoint) {
        return endpoint.returnType().is(Flux.class);
    }

    public static FluxResponseT provider() {
        return SINGLE_INSTANCE;
    }

}
