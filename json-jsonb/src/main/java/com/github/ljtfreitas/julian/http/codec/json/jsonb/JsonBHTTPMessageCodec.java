/*
 * Copyright (C) 2021 Tiago de Freitas Lima
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */

package com.github.ljtfreitas.julian.http.codec.json.jsonb;

import com.github.ljtfreitas.julian.JavaType;
import com.github.ljtfreitas.julian.http.DefaultHTTPRequestBody;
import com.github.ljtfreitas.julian.http.HTTPRequestBody;
import com.github.ljtfreitas.julian.http.HTTPResponseBody;
import com.github.ljtfreitas.julian.http.MediaType;
import com.github.ljtfreitas.julian.http.codec.HTTPRequestWriterException;
import com.github.ljtfreitas.julian.http.codec.HTTPResponseReaderException;
import com.github.ljtfreitas.julian.http.codec.JsonHTTPMessageCodec;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbException;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.http.HttpRequest.BodyPublisher;
import java.net.http.HttpRequest.BodyPublishers;
import java.nio.charset.Charset;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static com.github.ljtfreitas.julian.http.MediaType.APPLICATION_JSON;

public class JsonBHTTPMessageCodec implements JsonHTTPMessageCodec<Object> {

    private static final JsonBHTTPMessageCodec SINGLE_INSTANCE = new JsonBHTTPMessageCodec();

    private final Jsonb jsonb;

    public JsonBHTTPMessageCodec() {
        this(JsonbBuilder.create());
    }

    public JsonBHTTPMessageCodec(Jsonb jsonb) {
        this.jsonb = jsonb;
    }

    @Override
    public boolean writable(MediaType candidate, JavaType javaType) {
        return supports(candidate);
    }

    @Override
    public HTTPRequestBody write(Object body, Charset encoding) {
        return new DefaultHTTPRequestBody(APPLICATION_JSON, () -> serialize(body, encoding));
    }

    private BodyPublisher serialize(Object body, Charset encoding) {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream();
             OutputStreamWriter writer = new OutputStreamWriter(output, encoding)) {

            jsonb.toJson(body, writer);

            output.flush();

            return BodyPublishers.ofByteArray(output.toByteArray());

        } catch (JsonbException | IOException e) {
            throw new HTTPRequestWriterException("JSON serialization failed. Source: " + body, e);
        }
    }

    @Override
    public boolean readable(MediaType candidate, JavaType javaType) {
        return supports(candidate);
    }


    @Override
    public Optional<CompletableFuture<Object>> read(HTTPResponseBody body, JavaType javaType) {
        return body.readAsInputStream(s -> deserialize(s, javaType));
    }

    private Object deserialize(InputStream bodyAsStream, JavaType javaType) {
        try (InputStreamReader reader = new InputStreamReader(bodyAsStream);
            BufferedReader buffered = new BufferedReader(reader)) {

            return jsonb.fromJson(buffered, javaType.get());
        } catch (JsonbException | IOException e) {
            throw new HTTPResponseReaderException("JSON deserialization failed. The target type was: " + javaType, e);
        }
    }

    public static JsonBHTTPMessageCodec provider() {
        return SINGLE_INSTANCE;
    }
}
