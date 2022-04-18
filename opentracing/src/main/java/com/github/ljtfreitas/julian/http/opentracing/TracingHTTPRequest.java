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

package com.github.ljtfreitas.julian.http.opentracing;

import com.github.ljtfreitas.julian.JavaType;
import com.github.ljtfreitas.julian.Promise;
import com.github.ljtfreitas.julian.http.HTTPHeaders;
import com.github.ljtfreitas.julian.http.HTTPMethod;
import com.github.ljtfreitas.julian.http.HTTPRequest;
import com.github.ljtfreitas.julian.http.HTTPRequestBody;
import com.github.ljtfreitas.julian.http.HTTPResponse;
import io.opentracing.ScopeManager;
import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;
import io.opentracing.tag.Tags;

import java.net.URI;
import java.util.Map;
import java.util.Optional;

class TracingHTTPRequest<T> implements HTTPRequest<T> {

    private static final String COMPONENT_NAME = "julian-http-client";

    private final HTTPRequest<T> request;
    private final Tracer tracer;
    private final ScopeManager scopeManager;
    private final Span activeSpan;

    TracingHTTPRequest(Tracer tracer, HTTPRequest<T> request) {
        this(tracer, tracer.scopeManager(), tracer.activeSpan(), request);
    }

    private TracingHTTPRequest(Tracer tracer, ScopeManager scopeManager, Span activeSpan, HTTPRequest<T> request) {
        this.tracer = tracer;
        this.scopeManager = scopeManager;
        this.activeSpan = activeSpan;
        this.request = request;
    }

    @Override
    public JavaType returnType() {
        return request.returnType();
    }

    @Override
    public HTTPRequest<T> path(URI path) {
        return new TracingHTTPRequest<T>(tracer, scopeManager, activeSpan, request.path(path));
    }

    @Override
    public HTTPRequest<T> method(HTTPMethod method) {
        return new TracingHTTPRequest<>(tracer, scopeManager, activeSpan, request.method(method));
    }

    @Override
    public HTTPRequest<T> headers(HTTPHeaders headers) {
        return new TracingHTTPRequest<>(tracer, scopeManager, activeSpan, request.headers(headers));
    }

    @Override
    public HTTPRequest<T> body(HTTPRequestBody body) {
        return new TracingHTTPRequest<>(tracer, scopeManager, activeSpan, request.body(body));
    }

    @Override
    public URI path() {
        return request.path();
    }

    @Override
    public HTTPMethod method() {
        return request.method();
    }

    @Override
    public HTTPHeaders headers() {
        return request.headers();
    }

    @Override
    public Optional<HTTPRequestBody> body() {
        return request.body();
    }

    @Override
    public Promise<HTTPResponse<T>> execute() {
        Span span = tracer.buildSpan(request.method().name())
                .withTag(Tags.SPAN_KIND.getKey(), Tags.SPAN_KIND_CLIENT)
                .withTag(Tags.COMPONENT.getKey(), COMPONENT_NAME)
                .withTag(Tags.HTTP_URL, request.path().toString())
                .withTag(Tags.HTTP_METHOD, request.method().name())
                .start();

        Promise<HTTPResponse<T>> response = inject(span).execute()
                .onFailure(e -> { error(span, e); span.finish(); })
                .onSuccess(r -> { decorate(span, r); span.finish(); });

        return response;
    }

    private void decorate(Span span, HTTPResponse<T> response) {
        Tags.HTTP_STATUS.set(span, response.status().code());
    }

    private void error(Span span, Exception e) {
        Tags.ERROR.set(span, true);
        span.log(Map.of("event", Tags.ERROR.getKey(), "error.object", e));
    }

    private HTTPRequest<T> inject(Span span) {
        MutableHTTPHeadersInjector injector = new MutableHTTPHeadersInjector(request.headers());

        tracer.inject(span.context(), Format.Builtin.HTTP_HEADERS, injector);

        return request.headers(injector.headers());
    }
}
