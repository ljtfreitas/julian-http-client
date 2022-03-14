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

import java.net.URI;
import java.util.Objects;
import java.util.Optional;

import com.github.ljtfreitas.julian.JavaType;
import com.github.ljtfreitas.julian.Preconditions;

public class HTTPEndpoint {

    private final URI path;
    private final HTTPMethod method;
    private final HTTPHeaders headers;
    private final Body body;
    private final JavaType returnType;

    public HTTPEndpoint(URI path, HTTPMethod method) {
        this(path, method, HTTPHeaders.empty(), null);
    }

    public HTTPEndpoint(URI path, HTTPMethod method, HTTPHeaders headers) {
        this(path, method, headers, null);
    }

    public HTTPEndpoint(URI path, HTTPMethod method, HTTPHeaders headers, HTTPEndpoint.Body body) {
        this(path, method, headers, body, JavaType.none());
    }

    public HTTPEndpoint(URI path, HTTPMethod method, HTTPHeaders headers, HTTPEndpoint.Body body, JavaType returnType) {
        this.path = path;
        this.method = method;
        this.headers = headers;
        this.body = body;
        this.returnType = returnType;
    }

    public URI path() {
        return path;
    }

    public HTTPMethod method() {
        return method;
    }

    public HTTPHeaders headers() {
        return headers;
    }

    public Optional<HTTPEndpoint.Body> body() {
        return Optional.ofNullable(body);
    }

    public JavaType returnType() {
        return returnType;
    }

    @Override
    public String toString() {
        return new StringBuilder()
                .append("path: ")
                .append(path)
                .append("\n")
                .append("HTTP method: ")
                .append(method)
                .append("\n")
                .append("headers: ")
                .append(headers)
                .append("\n")
                .append("body: ")
                .append(body)
                .append("\n")
                .append("return type: ")
                .append(returnType)
                .append("\n")
                .toString();
    }

    public static class Body {

        private final Object value;
        private final JavaType javaType;
        private final MediaType contentType;

        public Body(Object value) {
            this(value, JavaType.valueOf(value.getClass()), (MediaType) null);
        }

        public Body(Object value, JavaType javaType, String contentType) {
            this(value, javaType, contentType == null ? null : MediaType.valueOf(Preconditions.nonNull(contentType)));
        }

        public Body(Object value, JavaType javaType, MediaType contentType) {
            this.value = value;
            this.javaType = javaType;
            this.contentType = contentType;
        }

        public Object content() {
            return value;
        }

        public JavaType javaType() {
            return javaType;
        }

        public Optional<MediaType> contentType() {
            return Optional.ofNullable(contentType);
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof Body) {
                Body that = (Body) o;
                return value.equals(that.value) && javaType.equals(that.javaType);
            } else return false;
        }

        @Override
        public int hashCode() {
            return Objects.hash(value, javaType);
        }

        @Override
        public String toString() {
            return new StringBuilder()
                    .append("content: ")
                    .append(content())
                    .append("\n")
                    .append("java type: ")
                    .append(javaType)
                    .toString();

        }
    }
}
