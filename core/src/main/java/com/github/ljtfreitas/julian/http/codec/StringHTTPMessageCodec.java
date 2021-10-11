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

import com.github.ljtfreitas.julian.Bracket;
import com.github.ljtfreitas.julian.JavaType;
import com.github.ljtfreitas.julian.http.DefaultHTTPRequestBody;
import com.github.ljtfreitas.julian.http.HTTPRequestBody;
import com.github.ljtfreitas.julian.http.MediaType;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.http.HttpRequest;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.List;

import static java.util.stream.Collectors.joining;

public class StringHTTPMessageCodec implements HTTPRequestWriter<String>, HTTPResponseReader<String> {

	public static final MediaType TEXT_PLAIN_MEDIA_TYPE = MediaType.valueOf("text/plain");

	public static final MediaType TEXT_WILDCARD_MEDIA_TYPE = MediaType.valueOf("text/*");

	private static final StringHTTPMessageCodec SINGLE_INSTANCE = new StringHTTPMessageCodec();

	private final Collection<MediaType> mediaTypes;

	public StringHTTPMessageCodec() {
		this(WildcardHTTPResponseReader.WILDCARD_MEDIA_TYPE, TEXT_WILDCARD_MEDIA_TYPE);
	}

	public StringHTTPMessageCodec(MediaType... mediaTypes) {
		this.mediaTypes = List.of(mediaTypes);
	}

	@Override
	public Collection<MediaType> contentTypes() {
		return mediaTypes;
	}

	@Override
	public boolean readable(MediaType candidate, JavaType javaType) {
		return supports(candidate) && javaType.is(String.class);
	}

	@Override
	public boolean writable(MediaType candidate, JavaType javaType) {
		return supports(candidate) && javaType.is(String.class);
	}

	@Override
	public String read(byte[] body, JavaType javaType) {
		return Bracket.acquire(() -> new BufferedReader(new InputStreamReader(new ByteArrayInputStream(body))))
				.map(reader -> reader.lines().collect(joining("\n")))
				.prop(HTTPResponseReaderException::new);
	}

	@Override
	public HTTPRequestBody write(String body, Charset encoding) {
		return new DefaultHTTPRequestBody(TEXT_PLAIN_MEDIA_TYPE, () -> HttpRequest.BodyPublishers.ofByteArray(body.getBytes(encoding)));
	}

	public static StringHTTPMessageCodec get() {
		return SINGLE_INSTANCE;
	}
}
