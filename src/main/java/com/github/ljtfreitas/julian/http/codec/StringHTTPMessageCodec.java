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

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.List;

import com.github.ljtfreitas.julian.Bracket;
import com.github.ljtfreitas.julian.JavaType;

import static java.util.stream.Collectors.joining;

public class StringHTTPMessageCodec implements HTTPRequestWriter<String>, HTTPResponseReader<String> {

	static final ContentType TEXT_CONTENT_TYPE = ContentType.valueOf("text/*");

	private final Collection<ContentType> contentTypes;

	public StringHTTPMessageCodec() {
		this(WildcardHTTPResponseReader.WILDCARD_CONTENT_TYPE, TEXT_CONTENT_TYPE);
	}

	public StringHTTPMessageCodec(ContentType... contentTypes) {
		this.contentTypes = List.of(contentTypes);
	}

	@Override
	public Collection<ContentType> contentTypes() {
		return contentTypes;
	}

	@Override
	public boolean readable(ContentType candidate, JavaType javaType) {
		return supports(candidate) && javaType.is(String.class);
	}

	@Override
	public boolean writable(ContentType candidate, Class<?> javaType) {
		return supports(candidate) && javaType.equals(String.class);
	}

	@Override
	public String read(InputStream body, JavaType javaType) {
		return Bracket.acquire(() -> new BufferedReader(new InputStreamReader(body)))
				.map(reader -> reader.lines().collect(joining("\n")))
				.prop(HTTPResponseReaderException::new);
	}

	@Override
	public byte[] write(String body) {
		return body.getBytes();
	}
}
