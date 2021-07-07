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

package com.github.ljtfreitas.julian;

import java.util.Optional;

import com.github.ljtfreitas.julian.http.HTTPResponse;

public class HeadersResponseT implements ResponseT<Headers, Void> {

    @Override
    public <A> ResponseFn<Headers, A> comp(Endpoint endpoint, ResponseFn<Void, A> fn) {
        return new ResponseFn<>() {

            @SuppressWarnings({"unchecked", "rawtypes"})
            @Override
            public Promise<Headers> run(RequestIO<A> request, Arguments arguments) {
                return request.execute().then(r -> {
                    Optional<HTTPResponse> httpResponse = r.as(HTTPResponse.class);
                    return httpResponse.map(HTTPResponse::headers)
                            .map(headers -> headers.all().stream()
                                    .reduce(Headers.empty(), (h, header) -> h.add(header.name(), header.values()), (a, b) -> b))
                            .orElseGet(Headers::empty);
                });
            }

            @Override
            public JavaType returnType() {
                return null;
            }
        };
    }

    @Override
    public JavaType adapted(Endpoint endpoint) {
        return JavaType.none();
    }

    @Override
    public boolean test(Endpoint endpoint) {
        return endpoint.returnType().is(Headers.class);
    }
}
