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

import com.github.ljtfreitas.julian.Attempt;
import com.github.ljtfreitas.julian.JavaType;
import com.github.ljtfreitas.julian.Promise;
import com.github.ljtfreitas.julian.Subscriber;
import com.github.ljtfreitas.julian.http.codec.HTTPResponseReaderException;
import com.github.ljtfreitas.julian.http.codec.HTTPResponseReaders;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import static com.github.ljtfreitas.julian.Message.format;
import static com.github.ljtfreitas.julian.http.HTTPHeader.CONTENT_TYPE;

public class RecoverableHTTPResponse<T> implements HTTPResponse<T> {

    private final FailureHTTPResponse<T> response;
    private final HTTPResponseReaders readers;

    public RecoverableHTTPResponse(FailureHTTPResponse<T> response, HTTPResponseReaders readers) {
        this.response = response;
        this.readers = readers;
    }

    public <Expected> HTTPResponse<Expected> recover(Class<? extends Expected> expected) {
        Promise<MediaType> contentTypeAsPromise = Attempt.run(() -> response.headers().select(CONTENT_TYPE)
                .map(HTTPHeader::value)
                .map(MediaType::valueOf)
                .orElseThrow(() -> new IllegalStateException("The HTTP response must contain a Content-Type header.")))
                .promise();

        JavaType expectedJavaType = JavaType.valueOf(expected);

        Promise<Expected> recovered = contentTypeAsPromise.bind(contentType -> response.asException().bodyAsPromise()
                        .bind(bodyAsBytes -> readers.select(contentType, expectedJavaType)
                            .orElseThrow(() -> new HTTPResponseReaderException(format("There is no a HTTPResponseReader able to convert {0} to {1}", contentType, expectedJavaType)))
                            .read(HTTPResponseBody.some(bodyAsBytes), expectedJavaType)
                            .map(Promise::pending)
                            .orElseGet(Promise::empty))
                .then(value -> expectedJavaType.<Expected> cast(value).orElseThrow()))
                .failure(e -> new UnrecoverableHTTPResponseException(response.asException(), e));

        return HTTPResponse.lazy(response.status(), response.headers(), recovered);
    }

    @Override
    public Attempt<T> body() {
        return response.body();
    }

    @Override
    public <R> R fold(Function<? super T, R> success, Function<? super HTTPResponseException, R> failure) {
        return response.fold(success, failure);
    }

    @Override
    public HTTPStatus status() {
        return response.status();
    }

    @Override
    public HTTPHeaders headers() {
        return response.headers();
    }

    @Override
    public <R> HTTPResponse<R> map(Function<? super T, R> fn) {
        return new RecoverableHTTPResponse<>(response.map(fn), readers);
    }

    @Override
    public <R> HTTPResponse<R> map(HTTPResponseFn<? super T, R> fn) {
        return new RecoverableHTTPResponse<>(response.map(fn), readers);
    }

    @Override
    public HTTPResponse<T> onSuccess(Consumer<? super T> fn) {
        return response.onSuccess(fn);
    }

    @Override
    public HTTPResponse<T> onSuccess(HTTPResponseConsumer<? super T> fn) {
        return response.onSuccess(fn);
    }

    @Override
    public HTTPResponse<T> subscribe(Subscriber<? super T, HTTPResponseException> subscriber) {
        return response.subscribe(subscriber);
    }

    @Override
    public HTTPResponse<T> subscribe(HTTPResponseSubscriber<? super T> subscriber) {
        return response.subscribe(subscriber);
    }

    @Override
    public HTTPResponse<T> onFailure(Consumer<? super HTTPResponseException> fn) {
        return response.onFailure(fn);
    }

    @Override
    public HTTPResponse<T> recover(Function<? super HTTPResponseException, T> fn) {
        return response.recover(fn);
    }

    @Override
    public HTTPResponse<T> recover(Class<? extends HTTPResponseException> expected, Function<? super HTTPResponseException, T> fn) {
        return response.recover(expected, fn);
    }

    @Override
    public HTTPResponse<T> recover(Predicate<? super HTTPResponseException> p, Function<? super HTTPResponseException, T> fn) {
        return response.recover(fn);
    }

    @Override
    public HTTPResponse<T> recover(HTTPStatusCode code, HTTPResponseFn<byte[], T> fn) {
        return response.recover(code, fn);
    }

    @Override
    public Promise<T> promise() {
        return response.promise();
    }

    @Override
    public String toString() {
        return response.status() + "\n" + response.headers();
    }
}
