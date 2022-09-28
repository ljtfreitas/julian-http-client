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

package com.github.ljtfreitas.julian.http;

import com.github.ljtfreitas.julian.Arguments;
import com.github.ljtfreitas.julian.Endpoint;
import com.github.ljtfreitas.julian.JavaType;
import com.github.ljtfreitas.julian.Kind;
import com.github.ljtfreitas.julian.Promise;
import com.github.ljtfreitas.julian.Response;
import com.github.ljtfreitas.julian.ResponseFn;
import com.github.ljtfreitas.julian.ResponseT;

import java.lang.reflect.Type;

public class HTTPResponseT implements ResponseT<Response<Object, ? extends Throwable>, HTTPResponse<Object>> {

    private static final HTTPResponseT SINGLE_INSTANCE = new HTTPResponseT();

    @Override
    public <A> ResponseFn<A, HTTPResponse<Object>> bind(Endpoint endpoint, ResponseFn<A, Response<Object, ? extends Throwable>> next) {
        return new ResponseFn<>() {

            @Override
            public Promise<HTTPResponse<Object>> run(Promise<? extends Response<A, ? extends Throwable>> response, Arguments arguments) {
                return next.run(response, arguments).then(r -> r.cast(new Kind<HTTPResponse<Object>>() {})
                        .orElseThrow(() -> new IllegalArgumentException("It was expected a HTTPResponse instance, but it was " + r)));
            }

            @Override
            public JavaType returnType() {
                return next.returnType();
            }
        };
    }

    @Override
    public JavaType adapted(Endpoint endpoint) {
        Type[] args = endpoint.returnType().parameterized()
                .map(p -> new Type[] { p.getActualTypeArguments()[0], Exception.class })
                .orElseGet(() -> new Type[]{ Object.class, Exception.class });
        return JavaType.parameterized(Response.class, args);
    }

    @Override
    public boolean test(Endpoint endpoint) {
        return endpoint.returnType().is(HTTPResponse.class);
    }

    public static HTTPResponseT get() {
        return SINGLE_INSTANCE;
    }
}
