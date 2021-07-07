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

import java.lang.reflect.InvocationHandler;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.github.ljtfreitas.julian.contract.Contract;
import com.github.ljtfreitas.julian.contract.ContractReader;
import com.github.ljtfreitas.julian.contract.DefaultContractReader;
import com.github.ljtfreitas.julian.http.DefaultHTTPResponseFailure;
import com.github.ljtfreitas.julian.http.HTTP;
import com.github.ljtfreitas.julian.http.HTTPHeadersResponseT;
import com.github.ljtfreitas.julian.http.HTTPResponseFailure;
import com.github.ljtfreitas.julian.http.HTTPStatusCodeResponseT;
import com.github.ljtfreitas.julian.http.HTTPStatusResponseT;
import com.github.ljtfreitas.julian.http.client.DefaultHTTPClient;
import com.github.ljtfreitas.julian.http.client.HTTPClient;
import com.github.ljtfreitas.julian.http.codec.ByteArrayHTTPResponseReader;
import com.github.ljtfreitas.julian.http.codec.HTTPMessageCodec;
import com.github.ljtfreitas.julian.http.codec.InputStreamHTTPResponseReader;
import com.github.ljtfreitas.julian.http.codec.NonReadableHTTPResponseReader;
import com.github.ljtfreitas.julian.http.codec.ScalarHTTPMessageCodec;
import com.github.ljtfreitas.julian.http.codec.StringHTTPMessageCodec;

public class ProxyBuilder {

    private ContractReader contractReader = null;

    private HTTPClient httpClient = null;
    private HTTPResponseFailure failure = null;

    private final ResponsesTs responseTs = new ResponsesTs();
    private final HTTPMessageCodecs codecs = new HTTPMessageCodecs();

    public class HTTPMessageCodecs {

        private final Collection<HTTPMessageCodec> codecs = new ArrayList<>();

        private com.github.ljtfreitas.julian.http.codec.HTTPMessageCodecs build() {
            return new com.github.ljtfreitas.julian.http.codec.HTTPMessageCodecs(codecs());
        }

        private Collection<HTTPMessageCodec> codecs() {
            return codecs.isEmpty() ? all() : Collections.unmodifiableCollection(codecs);
        }

        private Collection<HTTPMessageCodec> all() {
            return List.of(
                    new ByteArrayHTTPResponseReader(),
                    new InputStreamHTTPResponseReader(),
                    new NonReadableHTTPResponseReader(),
                    new ScalarHTTPMessageCodec(),
                    new StringHTTPMessageCodec());
        }
    }

    public class ResponsesTs {

        private final Collection<ResponseT<?, ?>> responses = new ArrayList<>();

        private Responses build() {
            return new Responses(responses());
        }

        private Collection<ResponseT<?, ?>> responses() {
            return responses.isEmpty() ? all() : Collections.unmodifiableCollection(responses);
        }

        private Collection<ResponseT<?,?>> all() {
            return List.of(
                    new CallableResponseT<>(),
                    new CollectionResponseT<>(),
                    new CompletionStageCallbackResponseT<>(),
                    new CompletionStageResponseT<>(),
                    new DefaultResponseT<>(),
                    new EnumerationResponseT<>(),
                    new FutureResponseT<>(),
                    new FutureTaskResponseT<>(),
                    new HeadersResponseT(),
                    new IterableResponseT<>(),
                    new IteratorResponseT<>(),
                    new ListIteratorResponseT<>(),
                    new OptionalResponseT<>(),
                    new PromiseResponseT<>(),
                    new PublisherResponseT<>(),
                    new QueueResponseT<>(),
                    new RunnableResponseT(),
                    new StreamResponseT<>(),
                    new HTTPHeadersResponseT(),
                    new HTTPStatusCodeResponseT(),
                    new HTTPStatusResponseT());
        }
    }

    public <T> T build(Class<? extends T> type) {
        return build(type, handler(type, null));
    }

    public <T> T build(Class<? extends T> type, String endpoint) {
        return build(type, handler(type, Except.run(() -> new URL(endpoint)).unsafe()));
    }

    public <T> T build(Class<? extends T> type, URL endpoint) {
        return build(type, handler(type, endpoint));
    }

    private <T> T build(Class<? extends T> type, InvocationHandler handler) {
        return new ProxyFactory<T>(type, type.getClassLoader(), handler).create();
    }

    private InvocationHandler handler(Class<?> javaClass, URL endpoint) {
        return new DefaultInvocationHandler(contract(javaClass, endpoint), runner());
    }

    private Contract contract(Class<?> javaClass, URL endpoint) {
        return contractReader().read(new Definition(javaClass, endpoint));
    }

    private ContractReader contractReader() {
        return contractReader == null ? new DefaultContractReader() : contractReader;
    }

    private RequestRunner runner() {
        return new RequestRunner(responseTs.build(), http());
    }

    private HTTP http() {
        return new HTTP(httpClient(), codecs.build(), failure());
    }

    private HTTPClient httpClient() {
        return httpClient == null ? new DefaultHTTPClient() : httpClient;
    }

    private HTTPResponseFailure failure() {
        return failure == null ? new DefaultHTTPResponseFailure() : failure;
    }
}
