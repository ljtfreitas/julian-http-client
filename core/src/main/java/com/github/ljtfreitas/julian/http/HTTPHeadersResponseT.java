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

import java.util.Optional;

import com.github.ljtfreitas.julian.Arguments;
import com.github.ljtfreitas.julian.Endpoint;
import com.github.ljtfreitas.julian.JavaType;
import com.github.ljtfreitas.julian.Promise;
import com.github.ljtfreitas.julian.RequestIO;
import com.github.ljtfreitas.julian.ResponseFn;
import com.github.ljtfreitas.julian.ResponseT;

public class HTTPHeadersResponseT implements ResponseT<Void, HTTPHeaders> {

    private static final HTTPHeadersResponseT SINGLE_INSTANCE = new HTTPHeadersResponseT();

    @Override
    public <A> ResponseFn<A, HTTPHeaders> bind(Endpoint endpoint, ResponseFn<A, Void> fn) {
        return new ResponseFn<>() {

            @SuppressWarnings({"unchecked", "rawtypes"})
            @Override
            public Promise<HTTPHeaders, ? extends Exception> run(RequestIO<A> request, Arguments arguments) {
                return request.execute().then(r -> {
                    Optional<HTTPResponse> httpResponse = r.as(HTTPResponse.class);
                    return httpResponse.map(HTTPResponse::headers).orElseGet(HTTPHeaders::empty);
                });
            }

            @Override
            public JavaType returnType() {
                return fn.returnType();
            }
        };
    }

    @Override
    public JavaType adapted(Endpoint endpoint) {
        return JavaType.none();
    }

    @Override
    public boolean test(Endpoint endpoint) {
        return endpoint.returnType().is(HTTPHeaders.class);
    }

    public static HTTPHeadersResponseT get() {
        return SINGLE_INSTANCE;
    }
}
