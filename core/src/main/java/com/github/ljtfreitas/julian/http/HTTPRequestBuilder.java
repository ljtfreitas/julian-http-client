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

import com.github.ljtfreitas.julian.Except;
import com.github.ljtfreitas.julian.JavaType;
import com.github.ljtfreitas.julian.Kind;
import com.github.ljtfreitas.julian.QueryParameters;
import com.github.ljtfreitas.julian.http.client.HTTPClient;
import com.github.ljtfreitas.julian.http.codec.HTTPMessageCodecs;
import com.github.ljtfreitas.julian.http.codec.HTTPRequestWriter;
import com.github.ljtfreitas.julian.http.codec.HTTPResponseReader;
import com.github.ljtfreitas.julian.http.codec.UnprocessableHTTPMessageCodec;

import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class HTTPRequestBuilder<T> {

    protected final String path;
    protected final HTTPMethod httpMethod;
    protected final QueryParameters parameters;
    protected final HTTPHeaders headers;
    protected final HTTPRequestBody body;
    protected final JavaType returnType;
    protected final HTTPResponseReader<?> reader;

    private HTTPRequestBuilder(String path, HTTPMethod httpMethod) {
        this(path, httpMethod, QueryParameters.empty());
    }

    private HTTPRequestBuilder(String path, HTTPMethod httpMethod, QueryParameters parameters) {
        this(path, httpMethod, parameters, HTTPHeaders.empty());
    }

    private HTTPRequestBuilder(String path, HTTPMethod httpMethod, QueryParameters parameters, HTTPHeaders headers) {
        this(path, httpMethod, parameters, headers, null);
    }

    private HTTPRequestBuilder(String path, HTTPMethod httpMethod, QueryParameters parameters, HTTPHeaders headers, HTTPRequestBody body) {
        this(path, httpMethod, parameters, headers, body, JavaType.none(), UnprocessableHTTPMessageCodec.get());
    }

    private HTTPRequestBuilder(String path, HTTPMethod httpMethod, QueryParameters parameters, HTTPHeaders headers, HTTPRequestBody body, JavaType responseType,
                               HTTPResponseReader<?> reader) {
        this.path = path;
        this.httpMethod = httpMethod;
        this.headers = headers;
        this.body = body;
        this.returnType = responseType;
        this.reader = reader;
        this.parameters = parameters;
    }

    public HTTPRequest<T> build(HTTPClient client) {
        HTTPMessageCodecs codecs = new HTTPMessageCodecs(List.of(reader));
        return new DefaultHTTPRequest<>(path(), httpMethod, body, headers, returnType, client, codecs, DefaultHTTPResponseFailure.get());
    }

    private URI path() {
        URI source = URI.create(path);

        String query = parameters.append(QueryParameters.parse(source.getQuery())).serialize();

        return Except.run(()->  new URI(source.getScheme(), source.getUserInfo(), source.getHost(), source.getPort(), source.getPath(),
                    query == null || query.isEmpty() ? null : query, source.getFragment())).prop();
    }

    public static class HasResponse<T> extends HTTPRequestBuilder<T> {

        private HasResponse(String path, HTTPMethod httpMethod) {
            super(path, httpMethod);
        }

        private HasResponse(String path, HTTPMethod httpMethod, QueryParameters parameters, HTTPHeaders headers) {
            super(path, httpMethod, parameters, headers);
        }

        public HasResponse(String path, HTTPMethod httpMethod, QueryParameters parameters, HTTPHeaders headers, HTTPRequestBody body) {
            super(path, httpMethod, parameters, headers, body);
        }

        public <R> HTTPRequestBuilder<R> response(Class<? extends R> expected, HTTPResponseReader<? extends R> reader) {
            return response(JavaType.valueOf(expected), reader);
        }

        public <R> HTTPRequestBuilder<R> response(Kind<R> expected, HTTPResponseReader<? extends R> reader) {
            return response(expected.javaType(), reader);
        }

        public <R> HTTPRequestBuilder<R> response(JavaType javaType, HTTPResponseReader<? extends R> reader) {
            return new HTTPRequestBuilder<>(path, httpMethod, parameters, headers, body, javaType, reader);
        }
    }

    public static class HasHeaders<T> extends HasResponse<T> {

        HasHeaders(String path, HTTPMethod httpMethod) {
            super(path, httpMethod);
        }

        private HasHeaders(String path, HTTPMethod httpMethod, QueryParameters parameters, HTTPHeaders headers) {
            super(path, httpMethod, parameters, headers);
        }

        public HasHeaders<T> header(HTTPHeader header) {
            return new HasHeaders<>(path, httpMethod, parameters, headers.join(header));
        }

        public HasHeaders<T> param(String name, String value) {
            return new HasHeaders<>(path, httpMethod, parameters.append(name, value), headers);
        }
    }

    public static class JustHeaders<T> {

        private final HasHeaders<T> hasHeaders;

        JustHeaders(String path, HTTPMethod httpMethod) {
            this(new HasHeaders<>(path, httpMethod));
        }

        private JustHeaders(HasHeaders<T> hasHeaders) {
            this.hasHeaders = hasHeaders;
        }

        public JustHeaders<T> header(HTTPHeader header) {
            return new JustHeaders<>(hasHeaders.header(header));
        }

        public JustHeaders<T> param(String name, String value) {
            return new JustHeaders<>(hasHeaders.param(name, value));
        }

        public HTTPRequest<T> build(HTTPClient client) {
            return hasHeaders.build(client);
        }
    }

    public static class HasBody<T> {

        private final HasHeaders<T> hasHeaders;

        HasBody(String path, HTTPMethod httpMethod) {
            this(new HasHeaders<>(path, httpMethod));
        }

        private HasBody(HasHeaders<T> hasHeaders) {
            this.hasHeaders = hasHeaders;
        }

        public <B> HasResponse<T> body(B value, HTTPRequestWriter<B> writer) {
            return body(value, StandardCharsets.UTF_8, writer);
        }

        public <B> HasResponse<T> body(B value, Charset charset, HTTPRequestWriter<B> writer) {
            return body(writer.write(value, charset));
        }

        public HasResponse<T> body(HTTPRequestBody body) {
            HTTPHeaders headers = body.contentType()
                    .map(c -> hasHeaders.headers.join(new HTTPHeader(HTTPHeader.CONTENT_TYPE, c.toString())))
                    .orElse(hasHeaders.headers);
            return new HasResponse<>(hasHeaders.path, hasHeaders.httpMethod, hasHeaders.parameters, headers, body);
        }

        public HasBody<T> header(HTTPHeader header) {
            return new HasBody<>(hasHeaders.header(header));
        }

        public HTTPRequest<T> build(HTTPClient client) {
            return hasHeaders.build(client);
        }
    }
}
