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

import com.github.ljtfreitas.julian.Message;
import com.github.ljtfreitas.julian.Promise;
import com.github.ljtfreitas.julian.http.HTTP;
import com.github.ljtfreitas.julian.http.HTTPEndpoint;
import com.github.ljtfreitas.julian.http.HTTPResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ReactiveHttpOutputMessage;
import org.springframework.web.reactive.function.BodyInserter;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Optional;

public class WebClientHTTP implements HTTP {

    private final WebClient webClient;

    public WebClientHTTP() {
        this(WebClient.create());
    }

    public WebClientHTTP(WebClient webClient) {
        this.webClient = webClient;
    }

    @Override
    public <T> Promise<HTTPResponse<T>> run(HTTPEndpoint endpoint) {
        HttpHeaders headers = endpoint.headers().all().stream()
                .reduce(new HttpHeaders(), (c, h) -> { c.addAll(h.name(), List.copyOf(h.values())); return c; }, (a, b) -> b);

        BodyInserter<?, ReactiveHttpOutputMessage> body = endpoint.body()
                .map(HTTPEndpoint.Body::content)
                .map(BodyInserters::fromValue).orElseGet(BodyInserters::empty);

        HttpMethod method = Optional.ofNullable(HttpMethod.resolve(endpoint.method().name()))
                .orElseThrow(() -> new IllegalArgumentException(Message.format("Unsupported HTTP method: {0}", endpoint.method())));

        return new WebClientHTTPRequest<T>(endpoint.path(), method, headers, body, endpoint.returnType(), webClient).execute();
    }
}
