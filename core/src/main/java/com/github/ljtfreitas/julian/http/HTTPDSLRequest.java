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

import java.net.URI;
import java.util.Arrays;
import java.util.Map;

import com.github.ljtfreitas.julian.Except;
import com.github.ljtfreitas.julian.JavaType;
import com.github.ljtfreitas.julian.Kind;
import com.github.ljtfreitas.julian.Promise;
import com.github.ljtfreitas.julian.QueryParameters;
import com.github.ljtfreitas.julian.http.HTTPEndpoint.Body;
import com.github.ljtfreitas.julian.http.auth.Authentication;

public class HTTPDSLRequest {

    private final HTTP http;
    private final URI path;
    private final HTTPMethod method;
    private final QueryParameters parameters;
    private final HTTPHeaders headers;
    private final Content body;

    public HTTPDSLRequest(HTTP http, URI path, HTTPMethod method) {
        this(http, path, method, QueryParameters.empty(), HTTPHeaders.empty(), null);
    }

    private HTTPDSLRequest(HTTP http, URI path, HTTPMethod method, QueryParameters parameters, HTTPHeaders headers, Content body) {
        this.http = http;
        this.path = path;
        this.parameters = parameters;
        this.method = method;
        this.headers = headers;
        this.body = body;
    }

    public HTTPDSLRequest parameter(String name, String...values) {
        return new HTTPDSLRequest(http, path, method, parameters.join(name, values), headers, body);
    }

    public HTTPDSLRequest parameters(Map<String, String> parameters) {
        return new HTTPDSLRequest(http, path, method,
                parameters.entrySet().stream()
                        .reduce(this.parameters, (p, e) -> p.join(e.getKey(), e.getValue()), (a, b) -> b),
                headers, body);
    }

    public HTTPDSLRequest header(String name, String... values) {
        return new HTTPDSLRequest(http, path, method, parameters,
                headers.join(Arrays.stream(values)
                        .reduce(HTTPHeaders.empty(), (h, value) -> h.join(new HTTPHeader(name, value)), (a, b) -> b)),
                body);
    }

    public HTTPDSLRequest header(HTTPHeader header) {
        return new HTTPDSLRequest(http, path, method, parameters, headers.join(header), body);
    }

    public HTTPDSLRequest headers(HTTPHeaders headers) {
        return new HTTPDSLRequest(http, path, method, parameters, headers.join(headers),
                body);
    }

    public HTTPDSLRequest authentication(Authentication authentication) {
        return new HTTPDSLRequest(http, path, method, parameters,
                headers.join(new HTTPHeader(HTTPHeader.AUTHORIZATION, authentication.content())),
                body);
    }

    public HTTPDSLRequest body(Object content) {
        return new HTTPDSLRequest(http, path, method, parameters, headers, new Content(content, null));
    }

    public HTTPDSLRequest body(Object content, MediaType contentType) {
        return new HTTPDSLRequest(http, path, method, parameters, headers, new Content(content, contentType));
    }

    public HTTPDSLRequest body(Object content, MediaType contentType, JavaType javaType) {
        return new HTTPDSLRequest(http, path, method, parameters, headers, new Content(content, contentType, javaType));
    }

    public HTTPDSLRequest body(Object content, MediaType contentType, Kind<?> javaType) {
        return new HTTPDSLRequest(http, path, method, parameters, headers, new Content(content, contentType, javaType.javaType()));
    }

    public Promise<HTTPResponse<Void>> run() {
        return run(JavaType.none());
    }

    public <T> Promise<HTTPResponse<T>> run(Class<T> expected) {
        return run(JavaType.valueOf(expected));
    }

    public <T> Promise<HTTPResponse<T>> run(Kind<T> expected) {
        return run(expected.javaType());
    }

    public <T> Promise<HTTPResponse<T>> run(JavaType expected) {
        HTTPEndpoint endpoint = new HTTPEndpoint(path(), method, headers, body == null ? null : body.out(), expected);
        return http.run(endpoint);
    }

    private URI path() {
        String query = parameters.join(QueryParameters.parse(path.getQuery())).serialize();

        return Except.run(()->  new URI(path.getScheme(), path.getUserInfo(), path.getHost(), path.getPort(), path.getPath(),
                query == null || query.isEmpty() ? null : query, path.getFragment())).prop();
    }

    public class Content {

        private final Object value;
        private final MediaType contentType;
        private final JavaType javaType;

        private Content(Object content, MediaType contentType) {
            this(content, contentType, JavaType.valueOf(content.getClass()));
        }

        private Content(Object value, MediaType contentType, JavaType javaType) {
            this.value = value;
            this.contentType = contentType;
            this.javaType = javaType;
        }

        public Body out() {
            return new Body(value, javaType, contentType);
        }
    }
}
