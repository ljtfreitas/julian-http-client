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

package com.github.ljtfreitas.julian.spring.autoconfigure;

import com.github.ljtfreitas.julian.ProxyBuilder;
import com.github.ljtfreitas.julian.ResponseT;
import com.github.ljtfreitas.julian.contract.ContractReader;
import com.github.ljtfreitas.julian.contract.EndpointMetadata;
import com.github.ljtfreitas.julian.http.HTTP;
import com.github.ljtfreitas.julian.http.HTTPRequestInterceptor;
import com.github.ljtfreitas.julian.http.HTTPResponseFailures;
import com.github.ljtfreitas.julian.http.client.HTTPClient;
import com.github.ljtfreitas.julian.http.codec.HTTPMessageCodec;

import java.net.URI;
import java.util.Collection;
import java.util.concurrent.Executor;
import java.util.function.Function;

import static java.util.Collections.emptyList;

public interface JulianHTTPClientSpecification {

    default URI endpoint() {
        return null;
    }

    default Executor executor() {
        return null;
    }

    default HTTP http() {
        return null;
    }

    default HTTPClient httpClient() {
        return null;
    }

    default HTTPClient.Specification httpClientSpec() {
        return new HTTPClient.Specification();
    }

    default Collection<HTTPRequestInterceptor> interceptors() {
        return emptyList();
    }

    default HTTPResponseFailures failures() {
        return HTTPResponseFailures.empty();
    }

    default ContractReader contractReader() {
        return null;
    }

    default Collection<Function<EndpointMetadata, EndpointMetadata>> contractExtensions() {
        return emptyList();
    }

    default Collection<ResponseT<?, ?>> responses() {
        return emptyList();
    }

    default Collection<HTTPMessageCodec> codecs() {
        return emptyList();
    }

    default ProxyBuilder customize(ProxyBuilder builder) {
        return builder;
    }
}
