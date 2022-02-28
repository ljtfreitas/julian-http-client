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

package com.github.ljtfreitas.julian.http.codec.json.gson;

import com.github.ljtfreitas.julian.JavaType;
import com.github.ljtfreitas.julian.http.DefaultHTTPRequestBody;
import com.github.ljtfreitas.julian.http.HTTPRequestBody;
import com.github.ljtfreitas.julian.http.HTTPResponseBody;
import com.github.ljtfreitas.julian.http.MediaType;
import com.github.ljtfreitas.julian.http.codec.HTTPRequestWriterException;
import com.github.ljtfreitas.julian.http.codec.HTTPResponseReaderException;
import com.github.ljtfreitas.julian.http.codec.JsonHTTPMessageCodec;
import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.http.HttpRequest.BodyPublisher;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodySubscriber;
import java.net.http.HttpResponse.BodySubscribers;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow.Publisher;

import static com.github.ljtfreitas.julian.http.MediaType.APPLICATION_JSON;

public class GsonJsonHTTPMessageCodec<T> implements JsonHTTPMessageCodec<T> {

    private static final GsonJsonHTTPMessageCodec<Object> SINGLE_INSTANCE = new GsonJsonHTTPMessageCodec<>();

    private final Gson gson;

    public GsonJsonHTTPMessageCodec() {
        this(new Gson());
    }

    public GsonJsonHTTPMessageCodec(Gson gson) {
        this.gson = gson;
    }

    @Override
    public boolean writable(MediaType candidate, JavaType javaType) {
        return supports(candidate);
    }

    @Override
    public HTTPRequestBody write(T body, Charset encoding) {
        return new DefaultHTTPRequestBody(APPLICATION_JSON, () -> serialize(body, encoding));
    }

    private BodyPublisher serialize(T body, Charset encoding) {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream();
             OutputStreamWriter writer = new OutputStreamWriter(output, encoding)) {

            gson.toJson(body, writer);
            writer.flush();

            return BodyPublishers.ofByteArray(output.toByteArray());
        } catch (JsonIOException | IOException e) {
            throw new HTTPRequestWriterException("JSON serialization failed. Source: " + body, e);
        }
    }

    @Override
    public boolean readable(MediaType candidate, JavaType javaType) {
        return supports(candidate);
    }

    @Override
    public Optional<CompletableFuture<T>> read(HTTPResponseBody body, JavaType javaType) {
        return body.readAsInputStream(s -> deserialize(s, javaType));
    }

    private T deserialize(InputStream bodyAsStream, JavaType javaType) {
        try (InputStreamReader reader = new InputStreamReader(bodyAsStream);
             BufferedReader buffered = new BufferedReader(reader)) {

            TypeToken<?> token = TypeToken.get(javaType.get());
            return gson.fromJson(buffered, token.getType());

        } catch (JsonIOException | JsonSyntaxException | IOException e) {
            throw new HTTPResponseReaderException("JSON deserialization failed. The target type was: " + javaType, e);
        }
    }

    public static GsonJsonHTTPMessageCodec<Object> provider() {
        return SINGLE_INSTANCE;
    }
}
