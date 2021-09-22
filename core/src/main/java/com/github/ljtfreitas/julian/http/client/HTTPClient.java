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

package com.github.ljtfreitas.julian.http.client;

import com.github.ljtfreitas.julian.http.HTTPRequestDefinition;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import java.net.ProxySelector;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;

public interface HTTPClient {

	HTTPClientRequest request(HTTPRequestDefinition request);

    class Specification {

        private final Duration connectionTimeout;
        private final Duration requestTimeout;
        private final Charset charset;
        private final ProxySelector proxySelector;
        private final Redirects redirects;
        private final SSL ssl;

        public Specification() {
            this.connectionTimeout = null;
            this.requestTimeout = null;
            this.charset = StandardCharsets.UTF_8;
            this.proxySelector = null;
            this.redirects = new Redirects();
            this.ssl = new SSL();
        }

        private Specification(Duration connectionTimeout, Duration requestTimeout, Charset charset, ProxySelector proxySelector, Redirects redirects, SSL ssl) {
            this.connectionTimeout = connectionTimeout;
            this.requestTimeout = requestTimeout;
            this.charset = charset;
            this.proxySelector = proxySelector;
            this.redirects = redirects;
            this.ssl = ssl;
        }

        public Specification connectionTimeout(Duration connectionTimeout) {
            return new Specification(this.connectionTimeout, this.requestTimeout, this.charset, this.proxySelector, this.redirects, this.ssl);
        }

        public Optional<Duration> connectionTimeout() {
            return Optional.ofNullable(connectionTimeout);
        }

        public Specification requestTimeout(Duration requestTimeout) {
            return new Specification(this.connectionTimeout, requestTimeout, this.charset, this.proxySelector, this.redirects, this.ssl);
        }

        public Optional<Duration> requestTimeout() {
            return Optional.ofNullable(requestTimeout);
        }

        public Specification charset(Charset charset) {
            return new Specification(this.connectionTimeout, this.requestTimeout, charset, this.proxySelector, this.redirects, this.ssl);
        }

        public Specification proxySelector(ProxySelector proxySelector) {
            return new Specification(this.connectionTimeout, this.requestTimeout, this.charset, proxySelector, this.redirects, this.ssl);
        }

        public Optional<ProxySelector> proxySelector() {
            return Optional.ofNullable(proxySelector);
        }

        public Specification.Redirects redirects() {
            return redirects;
        }

        private Specification redirects(Redirects redirects) {
            return new Specification(this.connectionTimeout, this.requestTimeout, this.charset, proxySelector, redirects, this.ssl);
        }

        public Specification.SSL ssl() {
            return ssl;
        }

        private Specification ssl(SSL ssl) {
            return new Specification(this.connectionTimeout, this.requestTimeout, this.charset, proxySelector, redirects, ssl);
        }

        public class Redirects {

            private final boolean follow;

            private Redirects() {
                this.follow = true;
            }

            private Redirects(boolean follow) {
                this.follow = follow;
            }

            public Specification follow() {
                return Specification.this;
            }

            public Specification nofollow() {
                return Specification.this.redirects(new Redirects(false));
            }

            public Specification follow(boolean follow) {
                return Specification.this.redirects(new Redirects(follow));
            }

            public void apply(Runnable whenFollow, Runnable whenNoFollow) {
                if (this.follow) whenFollow.run(); else whenNoFollow.run();
            }
        }

        public class SSL {

            private final SSLContext sslContext;
            private final SSLParameters parameters;

            private SSL() {
                this(null, null);
            }

            private SSL(SSLContext sslContext, SSLParameters parameters) {
                this.sslContext = sslContext;
                this.parameters = parameters;
            }

            public Specification context(SSLContext sslContext) {
                return Specification.this.ssl(new SSL(sslContext, this.parameters));
            }

            public Optional<SSLContext> context() {
                return Optional.ofNullable(sslContext);
            }

            public Specification parameters(SSLParameters parameters) {
                return Specification.this.ssl(new SSL(this.sslContext, parameters));
            }

            public Optional<SSLParameters> parameters() {
                return Optional.ofNullable(parameters);
            }
        }
    }
}
