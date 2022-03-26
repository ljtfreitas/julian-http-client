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

package com.github.ljtfreitas.julian.http.resilience4j;

import com.github.ljtfreitas.julian.JavaType;
import com.github.ljtfreitas.julian.Promise;
import com.github.ljtfreitas.julian.http.HTTPHeaders;
import com.github.ljtfreitas.julian.http.HTTPMethod;
import com.github.ljtfreitas.julian.http.HTTPRequest;
import com.github.ljtfreitas.julian.http.HTTPRequestBody;
import com.github.ljtfreitas.julian.http.HTTPResponse;
import io.github.resilience4j.timelimiter.TimeLimiter;

import java.net.URI;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;

import static io.github.resilience4j.ratelimiter.RateLimiter.waitForPermission;

class TimeLimiterHTTPRequest<T> implements HTTPRequest<T> {

    private final TimeLimiter timeLimiter;
    private final ScheduledExecutorService scheduler;
    private final HTTPRequest<T> request;

    TimeLimiterHTTPRequest(TimeLimiter timeLimiter, ScheduledExecutorService scheduler, HTTPRequest<T> request) {
        this.timeLimiter = timeLimiter;
        this.scheduler = scheduler;
        this.request = request;
    }

    @Override
    public JavaType returnType() {
        return request.returnType();
    }

    @Override
    public HTTPRequest<T> path(URI path) {
        return new TimeLimiterHTTPRequest<>(timeLimiter, scheduler, request.path(path));
    }

    @Override
    public HTTPRequest<T> method(HTTPMethod method) {
        return new TimeLimiterHTTPRequest<>(timeLimiter, scheduler, request.method(method));
    }

    @Override
    public HTTPRequest<T> headers(HTTPHeaders headers) {
        return new TimeLimiterHTTPRequest<>(timeLimiter, scheduler, request.headers(headers));
    }

    @Override
    public HTTPRequest<T> body(HTTPRequestBody body) {
        return new TimeLimiterHTTPRequest<>(timeLimiter, scheduler, request.body(body));
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
        return Promise.pending(timeLimiter.executeCompletionStage(scheduler, () -> request.execute().future()).toCompletableFuture());
    }
}
