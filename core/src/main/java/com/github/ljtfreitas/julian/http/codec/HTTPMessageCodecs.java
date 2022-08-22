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

import java.util.Collection;

import static java.util.stream.Collectors.toUnmodifiableList;

public class HTTPMessageCodecs {

	private final HTTPRequestWriters writers;
	private final HTTPResponseReaders readers;

	public HTTPMessageCodecs(Collection<? extends HTTPMessageCodec> codecs) {
		this.writers = new HTTPRequestWriters(writers(codecs));
		this.readers = new HTTPResponseReaders(readers(codecs));
	}

	private Collection<HTTPResponseReader<?>> readers(Collection<? extends HTTPMessageCodec> converters) {
		return converters.stream().filter(HTTPResponseReader.class::isInstance)
				.map(r -> (HTTPResponseReader<?>) r)
				.collect(toUnmodifiableList());
	}

	@SuppressWarnings("unchecked")
	private Collection<HTTPRequestWriter<? super Object>> writers(Collection<? extends HTTPMessageCodec> converters) {
		return converters.stream().filter(HTTPRequestWriter.class::isInstance)
				.map(HTTPRequestWriter.class::cast)
				.map(w -> (HTTPRequestWriter<? super Object>) w)
				.collect(toUnmodifiableList());
	}

	public HTTPRequestWriters writers() {
		return writers;
	}

	public HTTPResponseReaders readers() {
		return readers;
	}
}
