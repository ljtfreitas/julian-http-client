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
import com.github.ljtfreitas.julian.contract.DefaultEndpointMetadata;
import com.github.ljtfreitas.julian.contract.EndpointMetadata;
import com.github.ljtfreitas.julian.http.ConditionalHTTPResponseFailure;
import com.github.ljtfreitas.julian.http.DefaultHTTP;
import com.github.ljtfreitas.julian.http.DefaultHTTPResponseFailure;
import com.github.ljtfreitas.julian.http.HTTP;
import com.github.ljtfreitas.julian.http.HTTPHeadersResponseT;
import com.github.ljtfreitas.julian.http.HTTPRequestInterceptor;
import com.github.ljtfreitas.julian.http.HTTPRequestInterceptorChain;
import com.github.ljtfreitas.julian.http.HTTPResponseFailure;
import com.github.ljtfreitas.julian.http.HTTPStatusCode;
import com.github.ljtfreitas.julian.http.HTTPStatusCodeResponseT;
import com.github.ljtfreitas.julian.http.HTTPStatusGroup;
import com.github.ljtfreitas.julian.http.HTTPStatusResponseT;
import com.github.ljtfreitas.julian.http.client.ComposedHTTPClient;
import com.github.ljtfreitas.julian.http.client.DebugHTTPClient;
import com.github.ljtfreitas.julian.http.client.DefaultHTTPClient;
import com.github.ljtfreitas.julian.http.client.HTTPClient;
import com.github.ljtfreitas.julian.http.codec.ByteArrayHTTPMessageCodec;
import com.github.ljtfreitas.julian.http.codec.ByteBufferHTTPMessageCodec;
import com.github.ljtfreitas.julian.http.codec.HTTPMessageCodec;
import com.github.ljtfreitas.julian.http.codec.InputStreamHTTPMessageCodec;
import com.github.ljtfreitas.julian.http.codec.ScalarHTTPMessageCodec;
import com.github.ljtfreitas.julian.http.codec.StringHTTPMessageCodec;
import com.github.ljtfreitas.julian.http.codec.UnprocessableHTTPMessageCodec;
import com.github.ljtfreitas.julian.spi.Plugins;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import java.lang.reflect.InvocationHandler;
import java.net.ProxySelector;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.function.Function;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableCollection;
import static java.util.Objects.requireNonNull;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toUnmodifiableList;

public class ProxyBuilder {

    private final ContractSpec contractSpec = new ContractSpec();
    private final HTTPSpec httpSpec = new HTTPSpec();
    private final ResponsesTs responseTs = new ResponsesTs();
    private final HTTPMessageCodecs codecs = new HTTPMessageCodecs();

    private final Plugins plugins = new Plugins();

    private ClassLoader classLoader = null;

    public class HTTPMessageCodecs {

        private final Map<Class<? extends HTTPMessageCodec>, HTTPMessageCodec> codecs = new LinkedHashMap<>();

        public HTTPMessageCodecs add(HTTPMessageCodec... codecs) {
            return add(asList(codecs));
        }

        public HTTPMessageCodecs add(Collection<HTTPMessageCodec> codecs) {
            this.codecs.putAll(asMap(codecs));
            return this;
        }

        private Map<? extends Class<? extends HTTPMessageCodec>, HTTPMessageCodec> asMap(Collection<HTTPMessageCodec> codecs) {
            return codecs.stream().collect(toMap(HTTPMessageCodec::getClass, identity()));
        }

        public ProxyBuilder and() {
            return ProxyBuilder.this;
        }

        private com.github.ljtfreitas.julian.http.codec.HTTPMessageCodecs build() {
            return new com.github.ljtfreitas.julian.http.codec.HTTPMessageCodecs(codecs());
        }

        private Collection<HTTPMessageCodec> codecs() {
            Map<Class<? extends HTTPMessageCodec>, HTTPMessageCodec> all = new LinkedHashMap<>();
            all.putAll(plugins());
            all.putAll(all());
            all.putAll(codecs);
            return all.values();
        }

        private Map<? extends Class<? extends HTTPMessageCodec>, HTTPMessageCodec> plugins() {
            return asMap(plugins.all(HTTPMessageCodec.class).collect(toUnmodifiableList()));
        }

        private Map<? extends Class<? extends HTTPMessageCodec>, HTTPMessageCodec> all() {
            return asMap(List.of(
                    ByteArrayHTTPMessageCodec.get(),
                    ByteBufferHTTPMessageCodec.get(),
                    InputStreamHTTPMessageCodec.get(),
                    UnprocessableHTTPMessageCodec.get(),
                    ScalarHTTPMessageCodec.get(),
                    StringHTTPMessageCodec.get()));
        }
    }

    public class ResponsesTs {

        @SuppressWarnings("rawtypes")
        private final Map<Class<? extends ResponseT>, ResponseT<?, ?>> responses = new LinkedHashMap<>();
        private final Async async = new Async();

        public ResponsesTs add(ResponseT<?, ?>... responses) {
            return add(asList(responses));
        }

        public ResponsesTs add(Collection<ResponseT<?, ?>> responses) {
            this.responses.putAll(asMap(responses));
            return this;
        }

        @SuppressWarnings("rawtypes")
        private Map<? extends Class<? extends ResponseT>, ResponseT<?, ?>> asMap(Collection<ResponseT<?,?>> responses) {
            return responses.stream().collect(toMap(ResponseT::getClass, identity()));
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

        @SuppressWarnings("rawtypes")
        private Collection<ResponseT<?, ?>> responses() {
            Map<Class<? extends ResponseT>, ResponseT<?, ?>> all = new LinkedHashMap<>();
            all.putAll(plugins());
            all.putAll(all());
            all.putAll(asMap(async.all()));
            all.putAll(responses);
            return all.values();
        }

        @SuppressWarnings("rawtypes")
        private Map<? extends Class<? extends ResponseT>, ResponseT<?, ?>> plugins() {
            return asMap(plugins.all(ResponseT.class)
                    .map(r -> (ResponseT<?, ?>) r)
                    .collect(toUnmodifiableList()));
        }

        @SuppressWarnings("rawtypes")
        private Map<? extends Class<? extends ResponseT>, ResponseT<?, ?>> all() {
            return asMap(List.of(
                    CallableResponseT.get(),
                    CollectionResponseT.get(),
                    PromiseCallbackResponseT.get(),
                    CompletionStageResponseT.get(),
                    DefaultResponseT.get(),
                    EnumerationResponseT.get(),
                    ExceptResponseT.get(),
                    FutureResponseT.get(),
                    FutureTaskResponseT.get(),
                    HeadersResponseT.get(),
                    IterableResponseT.get(),
                    IteratorResponseT.get(),
                    LazyResponseT.get(),
                    ListIteratorResponseT.get(),
                    OptionalResponseT.get(),
                    PromiseResponseT.get(),
                    PromiseSubscriberCallbackResponseT.get(),
                    QueueResponseT.get(),
                    RunnableResponseT.get(),
                    StreamResponseT.get(),
                    HTTPHeadersResponseT.get(),
                    HTTPStatusCodeResponseT.get(),
                    HTTPStatusResponseT.get()));
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

            private Collection<ResponseT<?, ?>> all() {
                Collection<ResponseT<?, ?>> all = new ArrayList<>();
                all.add(executor == null ? PublisherResponseT.get() : new PublisherResponseT<>(executor));
                all.add(new SubscriberCallbackResponseT<>());
                return all;
            }
        }
    }

    public class HTTPSpec {

        private HTTP http = null;

        private final HTTPClientSpec client = new HTTPClientSpec();
        private final HTTPRequestInterceptors interceptors = new HTTPRequestInterceptors();
        private final HTTPResponseFailureSpec failure = new HTTPResponseFailureSpec();
        private final Encoding encoding = new Encoding();

        public ProxyBuilder with(HTTP http) {
            this.http = http;
            return ProxyBuilder.this;
        }

        public HTTPClientSpec client() {
            return client;
        }

        public HTTPRequestInterceptors interceptors() { return interceptors; }

        public HTTPResponseFailureSpec failure() {
            return failure;
        }

        public Encoding encoding() {
            return encoding;
        }

        public ProxyBuilder and() {
            return ProxyBuilder.this;
        }

        public HTTP build() {
            return http == null ?
                    new DefaultHTTP(client.build(), interceptors.build(), codecs.build(), failure.build(), encoding.charset) :
                    http;
        }

        public class HTTPClientSpec {

            private HTTPClient httpClient = null;

            private final Extensions extensions = new Extensions();
            private final Configuration configuration = new Configuration();

            public HTTPSpec with(HTTPClient httpClient) {
                this.httpClient = httpClient;
                return HTTPSpec.this;
            }

            public HTTPClientSpec.Extensions extensions() {
                return extensions;
            }

            public HTTPClientSpec.Configuration configure() { return configuration; }

            public HTTPSpec and() {
                return HTTPSpec.this;
            }

            private HTTPClient build() {
                HTTPClient client = this.httpClient == null ? new DefaultHTTPClient(configuration.specification) : this.httpClient;
                return extensions.apply(client);
            }

            public class Extensions {

                private final Debug debug = new Debug();

                public Extensions.Debug debug() {
                    return debug;
                }

                public HTTPClientSpec and() {
                    return HTTPClientSpec.this;
                }

                private HTTPClient apply(HTTPClient client) {
                    Collection<Function<HTTPClient, HTTPClient>> constructors = new ArrayList<>();
                    return new ComposedHTTPClient(client, debug.add(constructors));
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

                    public Extensions and() {
                        return Extensions.this;
                    }

                    private Collection<Function<HTTPClient, HTTPClient>> add(Collection<Function<HTTPClient, HTTPClient>> constructors) {
                        if (enabled) constructors.add(DebugHTTPClient::new);
                        return constructors;
                    }
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

                    private final HTTPClient.Specification specification;

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

        public class HTTPRequestInterceptors {

            private final Collection<HTTPRequestInterceptor> interceptors = new ArrayList<>();

            public HTTPRequestInterceptors add(HTTPRequestInterceptor... interceptors) {
                this.interceptors.addAll(asList(interceptors));
                return this;
            }

            public HTTPRequestInterceptors add(Collection<HTTPRequestInterceptor> interceptors) {
                this.interceptors.addAll(interceptors);
                return this;
            }

            public HTTPSpec and() {
                return HTTPSpec.this;
            }

            private HTTPRequestInterceptor build() {
                return new HTTPRequestInterceptorChain(unmodifiableCollection(interceptors));
            }
        }

        public class HTTPResponseFailureSpec {

            private HTTPResponseFailure failure = null;
            private final HTTPResponseFailures failures = new HTTPResponseFailures();

            public HTTPSpec with(HTTPResponseFailure failure) {
                this.failure = failure;
                return HTTPSpec.this;
            }

            public HTTPResponseFailureSpec when(HTTPStatusCode status, HTTPResponseFailure failure) {
                failures.add(status, failure);
                return this;
            }

            public HTTPResponseFailureSpec when(HTTPStatusGroup group, HTTPResponseFailure failure) {
                Arrays.stream(HTTPStatusCode.values()).filter(s -> s.onGroup(group)).forEach(s -> failures.add(s, failure));
                return this;
            }

            public HTTPSpec and() {
                return HTTPSpec.this;
            }

            private HTTPResponseFailure build() {
                return failures.has() ? failures.build() : failure == null ? DefaultHTTPResponseFailure.get() : failure;
            }

            private class HTTPResponseFailures {
                private final Map<HTTPStatusCode, HTTPResponseFailure> failures = new HashMap<>();

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

        public class Encoding {

            private Charset charset = StandardCharsets.UTF_8;

            public Encoding with(Charset charset) {
                this.charset = requireNonNull(charset);
                return this;
            }

            public Encoding with(String charset) {
                this.charset = Charset.forName(requireNonNull(charset));
                return this;
            }

            public HTTPSpec and() {
                return HTTPSpec.this;
            }
        }
    }

    public class ContractSpec {

        private final Extensions extensions = new Extensions();

        private ContractReader reader = null;

        public ContractSpec reader(ContractReader reader) {
            this.reader = reader;
            return this;
        }

        public Extensions extensions() {
            return extensions;
        }

        public ProxyBuilder and() {
            return ProxyBuilder.this;
        }

        private ContractReader build() {
            return reader == null ? new DefaultContractReader(endpointMetadata()) : reader;
        }

        private EndpointMetadata endpointMetadata() {
            return extensions.all.stream().reduce((EndpointMetadata) new DefaultEndpointMetadata(),
                    (m, f) -> f.apply(m), (a, b) -> b);
        }

        public class Extensions {

            private final Collection<Function<EndpointMetadata, EndpointMetadata>> all = new ArrayList<>();

            public Extensions apply(Function<EndpointMetadata, EndpointMetadata>... extensions) {
                all.addAll(asList(extensions));
                return this;
            }

            public Extensions apply(Collection<Function<EndpointMetadata, EndpointMetadata>> extensions) {
                all.addAll(extensions);
                return this;
            }

            public ContractSpec and() {
                return ContractSpec.this;
            }
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

    public HTTPMessageCodecs codecs() {
        return codecs;
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
        return new DefaultInvocationHandler(contract(javaClass, endpoint), client());
    }

    private Contract contract(Class<?> javaClass, URL endpoint) {
        return contractReader().read(new Definition(javaClass, endpoint));
    }

    private ContractReader contractReader() {
        return contractSpec.build();
    }

    private Client client() {
        HTTP http = httpSpec.build();
        return new Client(responseTs.build(), http);
    }
}