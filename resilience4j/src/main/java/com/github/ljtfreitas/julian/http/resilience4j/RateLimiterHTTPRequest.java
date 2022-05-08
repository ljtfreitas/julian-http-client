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

import com.github.ljtfreitas.julian.Attempt;
import com.github.ljtfreitas.julian.JavaType;
import com.github.ljtfreitas.julian.Promise;
import com.github.ljtfreitas.julian.http.HTTPClientFailureResponseException.TooManyRequests;
import com.github.ljtfreitas.julian.http.HTTPHeaders;
import com.github.ljtfreitas.julian.http.HTTPMethod;
import com.github.ljtfreitas.julian.http.HTTPRequest;
import com.github.ljtfreitas.julian.http.HTTPRequestBody;
import com.github.ljtfreitas.julian.http.HTTPResponse;
import io.github.resilience4j.core.exception.AcquirePermissionCancelledException;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;

import java.net.URI;
import java.util.Optional;

import static io.github.resilience4j.ratelimiter.RateLimiter.waitForPermission;

class RateLimiterHTTPRequest<T> implements HTTPRequest<T> {

    private final RateLimiter rateLimiter;
    private final HTTPRequest<T> request;

    RateLimiterHTTPRequest(RateLimiter rateLimiter, HTTPRequest<T> request) {
        this.rateLimiter = rateLimiter;
        this.request = request;
    }

    @Override
    public JavaType returnType() {
        return request.returnType();
    }

    @Override
    public HTTPRequest<T> path(URI path) {
        return new RateLimiterHTTPRequest<>(rateLimiter, request.path(path));
    }

    @Override
    public HTTPRequest<T> method(HTTPMethod method) {
        return new RateLimiterHTTPRequest<>(rateLimiter, request.method(method));
    }

    @Override
    public HTTPRequest<T> headers(HTTPHeaders headers) {
        return new RateLimiterHTTPRequest<>(rateLimiter, request.headers(headers));
    }

    @Override
    public HTTPRequest<T> body(HTTPRequestBody body) {
        return new RateLimiterHTTPRequest<>(rateLimiter, request.body(body));
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
        Attempt<Void> acquired = Attempt.just(() -> waitForPermission(rateLimiter));

        return acquired.map(none -> request.execute())
                .recover(RequestNotPermitted.class, () -> Promise.done(HTTPResponse.failed(new TooManyRequests(HTTPHeaders.empty(), Promise.done("Too many requests!".getBytes())))))
                .recover(AcquirePermissionCancelledException.class, () -> Promise.done(HTTPResponse.failed(new TooManyRequests(HTTPHeaders.empty(), Promise.done("Too many requests!".getBytes())))))
                .unsafe();
    }
}
