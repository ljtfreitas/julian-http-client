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

import com.github.ljtfreitas.julian.contract.Contract;
import com.github.ljtfreitas.julian.contract.ContractReader;
import com.github.ljtfreitas.julian.contract.DefaultContractReader;
import com.github.ljtfreitas.julian.http.ConditionalHTTPResponseFailure;
import com.github.ljtfreitas.julian.http.DebugHTTPClient;
import com.github.ljtfreitas.julian.http.DefaultHTTP;
import com.github.ljtfreitas.julian.http.DefaultHTTPResponseFailure;
import com.github.ljtfreitas.julian.http.HTTP;
import com.github.ljtfreitas.julian.http.HTTPHeadersResponseT;
import com.github.ljtfreitas.julian.http.HTTPRequestInterceptor;
import com.github.ljtfreitas.julian.http.HTTPRequestInterceptorChain;
import com.github.ljtfreitas.julian.http.HTTPResponseFailure;
import com.github.ljtfreitas.julian.http.HTTPStatusCode;
import com.github.ljtfreitas.julian.http.HTTPStatusCodeResponseT;
import com.github.ljtfreitas.julian.http.HTTPStatusResponseT;
import com.github.ljtfreitas.julian.http.InterceptedHTTP;
import com.github.ljtfreitas.julian.http.client.DefaultHTTPClient;
import com.github.ljtfreitas.julian.http.client.HTTPClient;
import com.github.ljtfreitas.julian.http.codec.ByteArrayHTTPResponseReader;
import com.github.ljtfreitas.julian.http.codec.HTTPMessageCodec;
import com.github.ljtfreitas.julian.http.codec.InputStreamHTTPResponseReader;
import com.github.ljtfreitas.julian.http.codec.NonReadableHTTPResponseReader;
import com.github.ljtfreitas.julian.http.codec.ScalarHTTPMessageCodec;
import com.github.ljtfreitas.julian.http.codec.StringHTTPMessageCodec;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import java.lang.reflect.InvocationHandler;
import java.net.ProxySelector;
import java.net.URL;
import java.nio.charset.Charset;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toUnmodifiableList;

public class ProxyBuilder {

    private final ContractSpec contractSpec = new ContractSpec();
    private final HTTPSpec httpSpec = new HTTPSpec();
    private final ResponsesTs responseTs = new ResponsesTs();
    private final HTTPMessageCodecs codecs = new HTTPMessageCodecs();

    private ClassLoader classLoader = null;

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
                    ByteArrayHTTPResponseReader.get(),
                    InputStreamHTTPResponseReader.get(),
                    NonReadableHTTPResponseReader.get(),
                    ScalarHTTPMessageCodec.get(),
                    StringHTTPMessageCodec.get());
        }
    }

    public class ResponsesTs {

        private final Collection<ResponseT<?, ?>> responses = new ArrayList<>();
        private final Async async = new Async();

        public ResponsesTs add(ResponseT<?, ?>... responses) {
            this.responses.addAll(Arrays.asList(responses));
            return this;
        }

        public ResponsesTs add(Collection<ResponseT<?, ?>> responses) {
            this.responses.addAll(responses);
            return this;
        }

        public Async async() {
            return async;
        }

        public ProxyBuilder and() {
            return ProxyBuilder.this;
        }

        private Responses build() {
            return new Responses(responses());
        }

        private Collection<ResponseT<?, ?>> responses() {
            return Stream.concat((responses.isEmpty() ? all() : Collections.unmodifiableCollection(responses)).stream(), async.all())
                    .collect(toUnmodifiableList());
        }

        private Collection<ResponseT<?,?>> all() {
            return List.of(
                    CallableResponseT.get(),
                    CollectionResponseT.get(),
                    CompletionStageCallbackResponseT.get(),
                    CompletionStageResponseT.get(),
                    DefaultResponseT.get(),
                    EnumerationResponseT.get(),
                    FutureResponseT.get(),
                    FutureTaskResponseT.get(),
                    HeadersResponseT.get(),
                    IterableResponseT.get(),
                    IteratorResponseT.get(),
                    ListIteratorResponseT.get(),
                    OptionalResponseT.get(),
                    PromiseResponseT.get(),
                    QueueResponseT.get(),
                    RunnableResponseT.get(),
                    StreamResponseT.get(),
                    HTTPHeadersResponseT.get(),
                    HTTPStatusCodeResponseT.get(),
                    HTTPStatusResponseT.get());
        }

        public class Async {

            private Executor executor = null;

            public Async executor(Executor executor) {
                this.executor = executor;
                return this;
            }

            public ResponsesTs and() {
                return ResponsesTs.this;
            }

            private Stream<ResponseT<?, ?>> all() {
                return Stream.of(executor == null ? PublisherResponseT.get() : new PublisherResponseT<>(executor));
            }
        }
    }

    public class HTTPSpec {

        private final HTTPClientSpec client = new HTTPClientSpec();
        private final HTTPResponseFailureSpec failure = new HTTPResponseFailureSpec();

        public HTTPClientSpec client() {
            return client;
        }

        public HTTPResponseFailureSpec failure() {
            return failure;
        }

        public ProxyBuilder and() {
            return ProxyBuilder.this;
        }

        public class HTTPClientSpec {

            private HTTPClient httpClient = null;

            private final Debug debug = new Debug();
            private final Interceptors interceptors = new Interceptors();
            private final Configuration configuration = new Configuration();

            public HTTPClientSpec using(HTTPClient httpClient) {
                this.httpClient = httpClient;
                return this;
            }

            public HTTPClientSpec.Debug debug() {
                return debug;
            }

            public HTTPClientSpec.Interceptors interceptors() { return interceptors; }

            public HTTPClientSpec.Configuration configure() { return configuration; }

            public HTTPSpec and() {
                return HTTPSpec.this;
            }

            private HTTPClient build() {
                HTTPClient client = this.httpClient == null ? new DefaultHTTPClient(configuration.specification) : this.httpClient;
                return debug.enabled ? new DebugHTTPClient(client) : client;
            }

            public class Debug {

                private boolean enabled = false;

                public Debug enabled() {
                    this.enabled = true;
                    return this;
                }

                public Debug disabled() {
                    this.enabled = false;
                    return this;
                }

                public Debug enabled(boolean enabled) {
                    this.enabled = enabled;
                    return this;
                }

                public HTTPClientSpec and() {
                    return HTTPClientSpec.this;
                }
            }

            public class Interceptors {

                private final Collection<HTTPRequestInterceptor> interceptors = new ArrayList<>();

                public Interceptors add(HTTPRequestInterceptor... interceptors) {
                    this.interceptors.addAll(Arrays.asList(interceptors));
                    return this;
                }

                public Interceptors add(Collection<HTTPRequestInterceptor> interceptors) {
                    this.interceptors.addAll(interceptors);
                    return this;
                }

                public HTTPClientSpec and() {
                    return HTTPClientSpec.this;
                }

                private Collection<HTTPRequestInterceptor> all() {
                    return interceptors;
                }
            }

            public class Configuration {

                private HTTPClient.Specification specification = new HTTPClient.Specification();
                private final Redirects redirects = new Redirects(specification);
                private final SSL ssl = new SSL(specification);

                public HTTPClientSpec.Configuration connectionTimeout(int connectionTimeout) {
                    this.specification = specification.connectionTimeout(Duration.ofMillis(connectionTimeout));
                    return this;
                }

                public HTTPClientSpec.Configuration connectionTimeout(Duration connectionTimeout) {
                    this.specification = specification.connectionTimeout(connectionTimeout);
                    return this;
                }

                public HTTPClientSpec.Configuration requestTimeout(int requestTimeout) {
                    this.specification = specification.requestTimeout(Duration.ofMillis(requestTimeout));
                    return this;
                }

                public HTTPClientSpec.Configuration requestTimeout(Duration requestTimeout) {
                    this.specification = specification.requestTimeout(requestTimeout);
                    return this;
                }

                public HTTPClientSpec.Configuration charset(Charset charset) {
                    this.specification = specification.charset(charset);
                    return this;
                }

                public HTTPClientSpec.Configuration charset(String charset) {
                    this.specification = specification.charset(Charset.forName(charset));
                    return this;
                }

                public HTTPClientSpec.Configuration proxySelector(ProxySelector proxySelector) {
                    this.specification = specification.proxySelector(proxySelector);
                    return this;
                }

                public HTTPClientSpec.Configuration.Redirects redirects() {
                    return redirects;
                }

                public HTTPClientSpec.Configuration.SSL ssl() {
                    return ssl;
                }

                private HTTPClientSpec.Configuration apply(HTTPClient.Specification specification) {
                    this.specification = specification;
                    return this;
                }

                public HTTPClientSpec and() {
                    return HTTPClientSpec.this;
                }

                public class Redirects {

                    private final HTTPClient.Specification specification;

                    private Redirects(HTTPClient.Specification specification) {
                        this.specification = specification;
                    }

                    public HTTPClientSpec.Configuration.Redirects follow() {
                        HTTPClientSpec.Configuration.this.apply(specification.redirects().follow());
                        return this;
                    }

                    public HTTPClientSpec.Configuration.Redirects nofollow() {
                        HTTPClientSpec.Configuration.this.apply(specification.redirects().nofollow());
                        return this;
                    }

                    public HTTPClientSpec.Configuration.Redirects follow(boolean follow) {
                        HTTPClientSpec.Configuration.this.apply(specification.redirects().follow(follow));
                        return this;
                    }

                    public HTTPClientSpec.Configuration and() {
                        return HTTPClientSpec.Configuration.this;
                    }
                }

                public class SSL {

                    private HTTPClient.Specification specification;

                    private SSL(HTTPClient.Specification specification) {
                        this.specification = specification;
                    }

                    public HTTPClientSpec.Configuration.SSL context(SSLContext sslContext) {
                        HTTPClientSpec.Configuration.this.apply(specification.ssl().context(sslContext));
                        return this;
                    }

                    public HTTPClientSpec.Configuration.SSL parameters(SSLParameters parameters) {
                        HTTPClientSpec.Configuration.this.apply(specification.ssl().parameters(parameters));
                        return this;
                    }

                    public HTTPClientSpec.Configuration and() {
                        return HTTPClientSpec.Configuration.this;
                    }
                }
            }
        }

        public class HTTPResponseFailureSpec {

            private HTTPResponseFailure failure = null;
            private final HTTPResponseFailures failures = new HTTPResponseFailures();

            public HTTPResponseFailureSpec using(HTTPResponseFailure failure) {
                this.failure = failure;
                return this;
            }

            public HTTPResponseFailureSpec when(HTTPStatusCode status, HTTPResponseFailure failure) {
                failures.add(status, failure);
                return this;
            }

            public HTTPSpec and() {
                return HTTPSpec.this;
            }

            public HTTPResponseFailure build() {
                return failures.has() ? failures.build() : failure == null ? DefaultHTTPResponseFailure.get() : failure;
            }

            public class HTTPResponseFailures {
                private Map<HTTPStatusCode, HTTPResponseFailure> failures = new HashMap<>();

                private void add(HTTPStatusCode status, HTTPResponseFailure failure) {
                    failures.put(status, failure);
                }

                private boolean has() {
                    return !failures.isEmpty();
                }

                private HTTPResponseFailure build() {
                    return new ConditionalHTTPResponseFailure(failures);
                }
            }
        }
    }

    public class ContractSpec {

        private ContractReader reader = null;

        public ContractSpec reader(ContractReader reader) {
            this.reader = reader;
            return this;
        }

        public ProxyBuilder and() {
            return ProxyBuilder.this;
        }

        public ContractReader build() {
            return reader == null ? new DefaultContractReader() : reader;
        }
    }

    public HTTPSpec http() {
        return httpSpec;
    }

    public ContractSpec contract() {
        return contractSpec;
    }

    public ResponsesTs responses() {
        return responseTs;
    }

    public ProxyBuilder classLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
        return this;
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
        return new ProxyFactory<T>(type, classLoader, handler).create();
    }

    private InvocationHandler handler(Class<?> javaClass, URL endpoint) {
        return new DefaultInvocationHandler(contract(javaClass, endpoint), runner());
    }

    private Contract contract(Class<?> javaClass, URL endpoint) {
        return contractReader().read(new Definition(javaClass, endpoint));
    }

    private ContractReader contractReader() {
        return contractSpec.build();
    }

    private RequestRunner runner() {
        HTTP http = intercepted(new DefaultHTTP(httpClient(), codecs.build(), failure()));
        return new RequestRunner(responseTs.build(), http);
    }

    private HTTPClient httpClient() {
        return httpSpec.client.build();
    }

    private HTTPResponseFailure failure() {
        return httpSpec.failure.build();
    }

    private HTTP intercepted(HTTP http) {
        return new InterceptedHTTP(http, interceptor());
    }

    private HTTPRequestInterceptor interceptor() {
        return new HTTPRequestInterceptorChain(httpSpec.client.interceptors.all());
    }
}
