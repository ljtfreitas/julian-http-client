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

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.InputStream;

import com.github.ljtfreitas.julian.JavaType;

public class InputStreamHTTPResponseReader implements WildcardHTTPResponseReader<InputStream> {

	private static final InputStreamHTTPResponseReader SINGLE_INSTANCE = new InputStreamHTTPResponseReader();

	private final ByteArrayHTTPResponseReader reader;

	public InputStreamHTTPResponseReader() {
		this.reader = ByteArrayHTTPResponseReader.get();
	}

	public InputStreamHTTPResponseReader(int bufferSize) {
		this.reader = new ByteArrayHTTPResponseReader(bufferSize);
	}

    @Override
	public boolean readable(ContentType candidate, JavaType javaType) {
		return supports(candidate) && javaType.is(InputStream.class);
	}

	@Override
	public InputStream read(InputStream body, JavaType javaType) {
		return new BufferedInputStream(new ByteArrayInputStream(reader.read(body, javaType)));
	}

    public static InputStreamHTTPResponseReader get() {
		return SINGLE_INSTANCE;
    }
}
