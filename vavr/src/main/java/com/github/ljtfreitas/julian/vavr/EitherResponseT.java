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
import com.github.ljtfreitas.julian.Promise;
import com.github.ljtfreitas.julian.Response;
import com.github.ljtfreitas.julian.ResponseFn;
import com.github.ljtfreitas.julian.ResponseT;
import io.vavr.control.Either;

public class EitherResponseT<L extends Exception> implements ResponseT<Object, Either<L, Object>> {

    @Override
    public <A> ResponseFn<A, Either<L, Object>> bind(Endpoint endpoint, ResponseFn<A, Object> fn) {
        return new ResponseFn<>() {

            @SuppressWarnings("unchecked")
            @Override
            public Promise<Either<L, Object>> run(Promise<? extends Response<A>> response, Arguments arguments) {
                Class<?> leftClassType = JavaType.valueOf(endpoint.returnType().parameterized().map(JavaType.Parameterized::firstArg).orElse(Exception.class)).rawClassType();

                return fn.run(response, arguments)
                        .then(Either::<L, Object>right)
                        .recover(leftClassType::isInstance, e -> Either.left((L) e));
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
                    .filter(left -> left.compatible(Exception.class))
                    .isPresent();
    }
}
