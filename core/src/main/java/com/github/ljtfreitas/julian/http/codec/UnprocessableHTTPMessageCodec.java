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

import com.github.ljtfreitas.julian.JavaType;
import com.github.ljtfreitas.julian.http.DefaultHTTPRequestBody;
import com.github.ljtfreitas.julian.http.HTTPRequestBody;
import com.github.ljtfreitas.julian.http.HTTPResponseBody;
import com.github.ljtfreitas.julian.http.MediaType;

import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodySubscriber;
import java.net.http.HttpResponse.BodySubscribers;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;
import java.util.concurrent.Flow.Publisher;

public class UnprocessableHTTPMessageCodec implements WildcardHTTPMessageCodec<Void> {

	private static final UnprocessableHTTPMessageCodec SINGLE_INSTANCE = new UnprocessableHTTPMessageCodec();

	private static final Set<Class<?>> SKIPPABLES_TYPES = new HashSet<>();

	static {
		SKIPPABLES_TYPES.add(void.class);
		SKIPPABLES_TYPES.add(Void.class);
	}

	@Override
	public boolean readable(MediaType candidate, JavaType javaType) {
		return supports(candidate) && javaType.classType().map(SKIPPABLES_TYPES::contains).orElse(false);
	}

	@Override
	public Optional<CompletableFuture<Void>> read(HTTPResponseBody body, JavaType javaType) {
		return body.content().map(this::deserialize);
	}

	private CompletableFuture<Void> deserialize(Publisher<List<ByteBuffer>> publisher) {
		BodySubscriber<Void> subscriber = BodySubscribers.discarding();
		publisher.subscribe(subscriber);
		return subscriber.getBody().toCompletableFuture();
	}

	@Override
	public boolean writable(MediaType candidate, JavaType javaType) {
		return supports(candidate) && javaType.classType().map(SKIPPABLES_TYPES::contains).orElse(false);
	}

	@Override
	public HTTPRequestBody write(Void body, Charset encoding) {
		return new DefaultHTTPRequestBody(HttpRequest.BodyPublishers::noBody);
	}

	public static UnprocessableHTTPMessageCodec get() {
		return SINGLE_INSTANCE;
	}
}
