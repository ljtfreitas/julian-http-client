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

package com.github.ljtfreitas.julian.http.spring.webflux;

import com.github.ljtfreitas.julian.JavaType;
import com.github.ljtfreitas.julian.Promise;
import com.github.ljtfreitas.julian.http.FailureHTTPResponse;
import com.github.ljtfreitas.julian.http.HTTPException;
import com.github.ljtfreitas.julian.http.HTTPFailureResponseException;
import com.github.ljtfreitas.julian.http.HTTPHeader;
import com.github.ljtfreitas.julian.http.HTTPHeaders;
import com.github.ljtfreitas.julian.http.HTTPRequestIO;
import com.github.ljtfreitas.julian.http.HTTPResponse;
import com.github.ljtfreitas.julian.http.HTTPResponseException;
import com.github.ljtfreitas.julian.http.HTTPStatus;
import com.github.ljtfreitas.julian.http.HTTPStatusCode;
import com.github.ljtfreitas.julian.http.HTTPUnknownFailureResponseException;
import com.github.ljtfreitas.julian.http.client.HTTPClientException;
import com.github.ljtfreitas.julian.http.codec.HTTPMessageException;
import com.github.ljtfreitas.julian.reactor.MonoPromise;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ReactiveHttpOutputMessage;
import org.springframework.web.reactive.function.BodyInserter;
import org.springframework.web.reactive.function.UnsupportedMediaTypeException;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;

class WebClientHTTPRequest<T> implements HTTPRequestIO<T> {

    private final URI path;
    private final HttpMethod method;
    private final HttpHeaders headers;
    private final BodyInserter<?, ReactiveHttpOutputMessage> body;
    private final JavaType responseType;
    private final WebClient webClient;

    public WebClientHTTPRequest(URI path, HttpMethod method, HttpHeaders headers, BodyInserter<?, ReactiveHttpOutputMessage> body, JavaType responseType, WebClient webClient) {
        this.path = path;
        this.method = method;
        this.headers = headers;
        this.body = body;
        this.responseType = responseType;
        this.webClient = webClient;
    }

    @Override
    public Promise<HTTPResponse<T>> execute() {
        Mono<HTTPResponse<T>> mono = webClient.method(method)
                .uri(path)
                .headers(h -> headers.forEach(h::addAll))
                .body(body)
                .exchangeToMono(this::read);

        return new MonoPromise<>(mono.onErrorMap(this::exceptionally));
    }

    Mono<HTTPResponse<T>> read(ClientResponse response) {
        HTTPStatus status = new HTTPStatus(response.rawStatusCode(), response.statusCode().getReasonPhrase());

        HTTPHeaders headers = response.headers().asHttpHeaders().entrySet().stream()
                .reduce(HTTPHeaders.empty(), (h, e) -> h.join(new HTTPHeader(e.getKey(), e.getValue())), (a, b) -> b);

        if (response.statusCode().isError()) {
            return response.createException().map(e -> failure(status, headers, e));

        } else {
            return success(status, headers, response);
        }
    }

    private HTTPResponse<T> failure(HTTPStatus status, HTTPHeaders headers, WebClientResponseException e) {
        HTTPResponseException exception = HTTPStatusCode.select(status.code())
                .<HTTPResponseException>map(httpStatusCode -> HTTPFailureResponseException.create(httpStatusCode, headers, e.getResponseBodyAsByteArray()))
                .orElseGet(() -> new HTTPUnknownFailureResponseException(status, headers, e.getResponseBodyAsByteArray()));

        return new FailureHTTPResponse<>(exception);
    }

    private HTTPException exceptionally(Throwable e) {
        if (e instanceof HTTPException) {
            return (HTTPException) e;

        } else if (e instanceof IOException) {
            return new HTTPClientException("I/O error: [" + method + " " + path + "]", e);

        } else if (e instanceof UnsupportedMediaTypeException) {
            return new HTTPMessageException("WebClient error: [" + method + " " + path + "]", e);

        } else if (e instanceof WebClientRequestException) {
            return new HTTPClientException("WebClient error: [" + method + " " + path + "]", e);

        } else if (e instanceof WebClientException) {
            return new HTTPClientException("WebClient error: [" + method + " " + path + "]", e);

        } else {
            return new HTTPException("Error on HTTP request: [" + method + " " + path + "]", e);
        }
    }

    private Mono<HTTPResponse<T>> success(HTTPStatus status, HTTPHeaders headers, ClientResponse response) {
        Mono<T> bodyAsMono = bodyToMono(response);

        return bodyAsMono.map(value -> HTTPResponse.success(status, headers, value))
                .switchIfEmpty(Mono.just(HTTPResponse.empty(status, headers)));
    }

    private Mono<T> bodyToMono(ClientResponse response) {
        // void/Void are special cases
        if (responseType.isNone()) {
            //noinspection unchecked
            return response.releaseBody().map(none -> (T) none);
        } else {
            ParameterizedTypeReference<T> expectedType = new ParameterizedTypeReference<>() {
                @Override
                public Type getType() {
                    return responseType.get();
                }
            };
            return response.bodyToMono(expectedType);
        }
    }
}
