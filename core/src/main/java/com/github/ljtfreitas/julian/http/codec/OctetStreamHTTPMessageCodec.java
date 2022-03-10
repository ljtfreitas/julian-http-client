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
import com.github.ljtfreitas.julian.http.HTTPResponseBody;
import com.github.ljtfreitas.julian.http.MediaType;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.http.HttpRequest.BodyPublishers;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow.Publisher;

public class OctetStreamHTTPMessageCodec implements HTTPRequestWriter<Serializable>, HTTPResponseReader<Serializable> {

	private static final Collection<MediaType> OCTET_STREAM_MEDIA_TYPES = List.of(MediaType.APPLICATION_OCTET_STREAM);

	@Override
	public Collection<MediaType> contentTypes() {
		return OCTET_STREAM_MEDIA_TYPES;
	}

	@Override
	public boolean writable(MediaType candidate, JavaType javaType) {
		return supports(candidate) && javaType.compatible(Serializable.class);
	}

	@Override
	public HTTPRequestBody write(Serializable body, Charset encoding) {
		return new DefaultHTTPRequestBody(MediaType.APPLICATION_OCTET_STREAM, () -> serialize(body));
	}

	private Publisher<ByteBuffer> serialize(Serializable body) {
		try (ByteArrayOutputStream output = new ByteArrayOutputStream();
			 ObjectOutputStream stream = new ObjectOutputStream(output)) {

			stream.writeObject(body);

			stream.flush();
			output.flush();

			return BodyPublishers.ofByteArray(output.toByteArray());

		} catch (IOException e) {
			throw new HTTPRequestWriterException("Object serialization failed. Source: " + body, e);
		}
	}

	@Override
	public boolean readable(MediaType candidate, JavaType javaType) {
		return supports(candidate) && javaType.compatible(Serializable.class);
	}

	@Override
	public Optional<CompletableFuture<Serializable>> read(HTTPResponseBody body, JavaType javaType) {
		return body.readAsInputStream(s -> deserialize(s, javaType));
	}

	private Serializable deserialize(InputStream bodyAsInputStream, JavaType javaType) {
		return Bracket.acquire(() -> new ObjectInputStream(bodyAsInputStream))
				.map(s -> (Serializable) s.readObject())
				.prop(e -> new HTTPResponseReaderException("Object deserialization failed. The target type was: " + javaType, e));
	}
}
