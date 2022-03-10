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

package com.github.ljtfreitas.julian.http.codec;

import java.io.InputStream;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodySubscriber;
import java.net.http.HttpResponse.BodySubscribers;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow.Publisher;

import com.github.ljtfreitas.julian.JavaType;
import com.github.ljtfreitas.julian.http.DefaultHTTPRequestBody;
import com.github.ljtfreitas.julian.http.HTTPRequestBody;
import com.github.ljtfreitas.julian.http.HTTPResponseBody;
import com.github.ljtfreitas.julian.http.MediaType;

public class InputStreamHTTPMessageCodec implements WildcardHTTPMessageCodec<InputStream> {

	private static final int DEFAULT_BUFFER_SIZE = 1024 * 100;

	private static final InputStreamHTTPMessageCodec SINGLE_INSTANCE = new InputStreamHTTPMessageCodec();

	private final ByteArrayHTTPMessageCodec reader;
	private final int bufferSize;

	public InputStreamHTTPMessageCodec() {
		this.reader = ByteArrayHTTPMessageCodec.get();
		this.bufferSize = DEFAULT_BUFFER_SIZE;
	}

	public InputStreamHTTPMessageCodec(int bufferSize) {
		this.reader = new ByteArrayHTTPMessageCodec(bufferSize);
		this.bufferSize = bufferSize;
	}

	@Override
	public boolean readable(MediaType candidate, JavaType javaType) {
		return supports(candidate) && javaType.is(InputStream.class);
	}

	@Override
	public Optional<CompletableFuture<InputStream>> read(HTTPResponseBody body, JavaType javaType) {
		return body.content().map(this::subscribe);
	}

	private CompletableFuture<InputStream> subscribe(Publisher<List<ByteBuffer>> publisher) {
		BodySubscriber<InputStream> subscriber = BodySubscribers.buffering(BodySubscribers.ofInputStream(), bufferSize);
		publisher.subscribe(subscriber);
		return subscriber.getBody().toCompletableFuture();
	}

	@Override
	public boolean writable(MediaType candidate, JavaType javaType) {
		return supports(candidate) && javaType.is(InputStream.class);
	}

	@Override
	public HTTPRequestBody write(InputStream body, Charset encoding) {
		return new DefaultHTTPRequestBody(() -> BodyPublishers.ofInputStream(() -> body));
	}

	public static InputStreamHTTPMessageCodec get() {
		return SINGLE_INSTANCE;
    }
}
