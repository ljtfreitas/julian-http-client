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

import java.net.URI;
import java.util.Objects;
import java.util.Optional;

public class RequestDefinition implements Request {

    private final URI path;
    private final String method;
    private final Headers headers;
    private final Body body;
    private final JavaType returnType;

    public RequestDefinition(URI path, String method) {
        this(path, method, Headers.empty());
    }

    public RequestDefinition(URI path, String method, JavaType returnType) {
        this(path, method, Headers.empty(), null, returnType);
    }

    public RequestDefinition(URI path, String method, Headers headers) {
        this(path, method, headers, null, JavaType.none());
    }

    public RequestDefinition(URI path, String method, Headers headers, JavaType returnType) {
        this(path, method, headers, null, returnType);
    }

    public RequestDefinition(URI path, String method, Headers headers, Body body) {
        this(path, method, headers, body, JavaType.none());
    }

    public RequestDefinition(URI path, String method, Headers headers, Body body, JavaType returnType) {
        this.path = path;
        this.method = method;
        this.headers = headers;
        this.body = body;
        this.returnType = returnType;
    }

    public URI path() {
        return path;
    }

    public String method() {
        return method;
    }

    public Headers headers() {
        return headers;
    }

    public Optional<Body> body() {
        return Optional.ofNullable(body);
    }

    @Override
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

        public Body(Object value) {
            this(value, JavaType.valueOf(value.getClass()));
        }

        public Body(Object value, JavaType javaType) {
            this.value = value;
            this.javaType = javaType;
        }

        public Object content() {
            return value;
        }

        public JavaType javaType() {
            return javaType;
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
