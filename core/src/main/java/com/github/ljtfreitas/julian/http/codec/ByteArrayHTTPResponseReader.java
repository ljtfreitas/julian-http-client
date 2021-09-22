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

import com.github.ljtfreitas.julian.Except;
import com.github.ljtfreitas.julian.JavaType;

public class ByteArrayHTTPResponseReader implements WildcardHTTPResponseReader<byte[]> {

	private static final ByteArrayHTTPResponseReader SINGLE_INSTANCE = new ByteArrayHTTPResponseReader();

	private static final int DEFAULT_BUFFER_SIZE = 1024 * 100;

	private final int bufferSize;

	public ByteArrayHTTPResponseReader() {
		this(DEFAULT_BUFFER_SIZE);
	}

	public ByteArrayHTTPResponseReader(int bufferSize) {
		this.bufferSize = bufferSize;
	}

    @Override
	public boolean readable(ContentType candidate, JavaType javaType) {
		return supports(candidate) && javaType.is(byte[].class);
	}

	@Override
	public byte[] read(InputStream body, JavaType javaType) {
		return Except.run(() -> body.readNBytes(bufferSize)).prop(HTTPResponseReaderException::new);
	}

    public static ByteArrayHTTPResponseReader get() {
		return SINGLE_INSTANCE;
    }
}
