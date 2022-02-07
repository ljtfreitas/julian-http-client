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

import java.io.InputStream;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodySubscriber;
import java.net.http.HttpResponse.BodySubscribers;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow.Publisher;
import java.util.function.Function;

import static com.github.ljtfreitas.julian.Preconditions.nonNull;
import static java.net.http.HttpResponse.BodySubscribers.mapping;

class PublisherHTTPResponseBody implements HTTPResponseBody {

	private final Publisher<List<ByteBuffer>> publisher;

	PublisherHTTPResponseBody(Publisher<List<ByteBuffer>> publisher) {
		this.publisher = nonNull(publisher);
	}

	@Override
	public <T> Optional<CompletableFuture<T>> readAsBytes(Function<byte[], T> fn) {
		BodySubscriber<T> subscriber = mapping(BodySubscribers.ofByteArray(), fn);
		publisher.subscribe(subscriber);
		return Optional.of(subscriber.getBody().toCompletableFuture());
	}

	@Override
	public <T> Optional<CompletableFuture<T>> readAsInputStream(Function<InputStream, T> fn) {
		BodySubscriber<T> subscriber = mapping(BodySubscribers.ofInputStream(), fn);
		publisher.subscribe(subscriber);
		return Optional.of(subscriber.getBody().toCompletableFuture());
	}

	@Override
	public Optional<Publisher<List<ByteBuffer>>> content() {
		return Optional.of(publisher);
	}
}
