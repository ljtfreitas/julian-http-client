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

package com.github.ljtfreitas.julian.vavr;

import com.github.ljtfreitas.julian.Arguments;
import com.github.ljtfreitas.julian.Endpoint;
import com.github.ljtfreitas.julian.JavaType;
import com.github.ljtfreitas.julian.Kind;
import com.github.ljtfreitas.julian.Promise;
import com.github.ljtfreitas.julian.Response;
import com.github.ljtfreitas.julian.ResponseFn;
import com.github.ljtfreitas.julian.ResponseT;
import com.github.ljtfreitas.julian.http.HTTPResponse;
import com.github.ljtfreitas.julian.http.HTTPResponseException;
import io.vavr.control.Either;

public class EitherHTTPResponseT<T> implements ResponseT<T, Either<HTTPResponseException, T>> {

    @Override
    public <A> ResponseFn<A, Either<HTTPResponseException, T>> bind(Endpoint endpoint, ResponseFn<A, T> fn) {
        return new ResponseFn<>() {

            @Override
            public Promise<Either<HTTPResponseException, T>, ? extends Exception> run(Promise<? extends Response<A, ? extends Exception>, ? extends Exception> response, Arguments arguments) {
                Promise<Response<T, ? extends Exception>, ? extends Exception> promise = response.then(r -> r.map(value -> fn.join(Promise.done(r), arguments)));

                return promise.then(r -> r.cast(new Kind<HTTPResponse<T>>() {}).orElseThrow())
                              .then(h -> h.fold(Either::right, Either::left));
            }

            @Override
            public JavaType returnType() {
                return fn.returnType();
            }
        };
    }

    @Override
    public JavaType adapted(Endpoint endpoint) {
        return JavaType.valueOf(endpoint.returnType().parameterized().map(p -> p.getActualTypeArguments()[1]).orElse(Object.class));
    }

    @Override
    public boolean test(Endpoint endpoint) {
        return endpoint.returnType().is(Either.class)
            && endpoint.returnType().parameterized()
                    .map(p -> p.getActualTypeArguments()[0])
                    .map(JavaType::valueOf)
                    .filter(left -> left.compatible(HTTPResponseException.class))
                    .isPresent();
    }
}