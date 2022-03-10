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

import java.io.ByteArrayInputStream;
import java.net.http.HttpRequest.BodyPublishers;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import com.github.ljtfreitas.julian.Bracket;
import com.github.ljtfreitas.julian.JavaType;
import com.github.ljtfreitas.julian.http.DefaultHTTPRequestBody;
import com.github.ljtfreitas.julian.http.HTTPRequestBody;
import com.github.ljtfreitas.julian.http.HTTPResponseBody;
import com.github.ljtfreitas.julian.http.MediaType;

public class ByteBufferHTTPMessageCodec implements WildcardHTTPMessageCodec<ByteBuffer> {

	private static final ByteBufferHTTPMessageCodec SINGLE_INSTANCE = new ByteBufferHTTPMessageCodec();

	private static final int DEFAULT_BUFFER_SIZE = 1024 * 100;

	private final int bufferSize;

	public ByteBufferHTTPMessageCodec() {
		this(DEFAULT_BUFFER_SIZE);
	}

	public ByteBufferHTTPMessageCodec(int bufferSize) {
		this.bufferSize = bufferSize;
	}

	@Override
	public boolean readable(MediaType candidate, JavaType javaType) {
		return supports(candidate) && javaType.is(ByteBuffer.class);
	}

	@Override
	public Optional<CompletableFuture<ByteBuffer>> read(HTTPResponseBody body, JavaType javaType) {
		return body.readAsBytes(bodyAsBytes -> Bracket.acquire(() -> new ByteArrayInputStream(bodyAsBytes))
				.map(stream -> stream.readNBytes(bufferSize))
				.map(ByteBuffer::wrap)
				.prop(HTTPResponseReaderException::new));
	}

	@Override
	public boolean writable(MediaType candidate, JavaType javaType) {
		return supports(candidate) && javaType.is(ByteBuffer.class);
	}

	@Override
	public HTTPRequestBody write(ByteBuffer body, Charset encoding) {
		return new DefaultHTTPRequestBody(() -> BodyPublishers.ofByteArray(read(body)));
	}

	private byte[] read(ByteBuffer body) {
		ByteBuffer buffer = body.flip();
		byte[] bytes = new byte[buffer.remaining()];
		buffer.get(bytes);
		return bytes;
	}

	public static ByteBufferHTTPMessageCodec get() {
		return SINGLE_INSTANCE;
    }
}
